/*
 *  Copyright 2024-2025 NetCracker Technology Corporation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.qubership.atp.mia.repo.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.Nonnull;

import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;
import org.qubership.atp.integration.configuration.mdc.MdcUtils;
import org.qubership.atp.mia.exceptions.MiaException;
import org.qubership.atp.mia.exceptions.businesslogic.ssh.SshException;
import org.qubership.atp.mia.exceptions.businesslogic.ssh.SshExecutionTimeoutException;
import org.qubership.atp.mia.exceptions.businesslogic.ssh.SshExecutionWrongExitException;
import org.qubership.atp.mia.exceptions.businesslogic.ssh.SshSftpException;
import org.qubership.atp.mia.model.environment.ConnectionProps;
import org.qubership.atp.mia.repo.impl.pool.ssh.ChannelType;
import org.qubership.atp.mia.service.MiaContext;
import org.qubership.atp.mia.utils.FileUtils;
import org.qubership.atp.mia.utils.Utils;
import org.slf4j.MDC;

import clover.com.google.common.base.Strings;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SshConnectionManager {

    private static final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(100);
    private static final AtomicLong instancesCounter = new AtomicLong();
    private static final String INTERRUPT_SIGNAL = "INT";
    private final ConnectionProps properties;
    private final String managerInstanceNumber;
    private final String externalPrefix;
    private final boolean saveFilesToWorkingDir;
    private final SshSession sshSession;
    private final MiaContext miaContext;
    private StringBuilder commandToExecute;

    /**
     * Creates instance of {@code SshConnectionManager}.
     * It's recommended to use
     * <pre>
     * {@code
     * ConnectionPool#getConnection(Server, String, FlowData)}
     * </pre>
     * instead of a manual creation.
     */
    public SshConnectionManager(SshSession session, String extPrefix, MiaContext miaContext) {
        this.miaContext = miaContext;
        sshSession = session;
        properties = session.getProperties();
        externalPrefix = extPrefix;
        saveFilesToWorkingDir = miaContext.getConfig().getCommonConfiguration().isSaveFilesToWorkingDir();
        managerInstanceNumber = "ssh_conn_manager_N_" + (instancesCounter.incrementAndGet());
        log.trace("Ssh manager №{} created and has environment properties: {}.", managerInstanceNumber,
                properties.fullInfo());
    }

    /**
     * Executes SSH command with external environment specific.
     *
     * @param command command to execute
     * @return output of command execution
     */
    public synchronized String runCommand(@Nonnull String command) {
        final StringBuffer output = new StringBuffer();
        final String prefix = isPrefixPresent() ? externalPrefix + "\n" : "";
        commandToExecute = new StringBuilder(command);
        if (command.contains("pbrun -u infinys")) {
            commandToExecute.append("'");
        }
        log.info("Execute ssh command:\n{}{}", prefix, commandToExecute);
        final String stopCode = "STOP " + UUID.randomUUID();
        final String echoStopCode = "echo \"" + stopCode + "\"";
        final String finalCommand = commandToExecute.toString() + "\n" + echoStopCode + "\n exit";
        AtomicBoolean isExecutedFlag = new AtomicBoolean(false);
        channelFlow(ChannelType.SHELL, false, "Could not run command [" + command + "]", (channel) -> {
            final ChannelShell channelShell = (ChannelShell) channel;
            channelShell.setPty(properties.isPty());
            channelShell.setExtOutputStream(new PipedOutputStream());
            final long start = System.currentTimeMillis();
            channelShell.connect(properties.getTimeoutConnect());
            final ScheduledFuture<?> future = interruptExecutionOnTimeout(command, isExecutedFlag, channelShell);
            try (final PrintStream input = new PrintStream(channelShell.getOutputStream())) {
                input.print(prefix);
                input.print("\n");
                input.print(finalCommand);
                input.print("\n");
                try (InputStream inputStream = channelShell.getInputStream()) {
                    try (InputStreamReader inputStreamReader = new InputStreamReader(inputStream)) {
                        try (BufferedReader reader = new BufferedReader(inputStreamReader)) {
                            String line;
                            input.flush();
                            int i = 0;
                            while ((line = reader.readLine()) != null) {
                                log.trace("{}) Line output: {}", ++i, line);
                                System.out.printf("%d) Line output: %s%n", ++i, line);
                                //Incorrect exit when command is wrong
                                if (line.matches("> " + echoStopCode)) {
                                    log.trace("{}) Line has {}: {}", i, "Incorrect command", line);
                                    throw new SshExecutionWrongExitException(managerInstanceNumber, finalCommand);
                                }
                                //Correct exit
                                if (line.equals(stopCode)) {
                                    log.trace("{}) Line correct exit: {}", i, line);
                                    System.out.printf("%d) Line correct exit: %s%n", ++i, line);
                                    break;
                                }
                                output.append(line).append("\n");
                            }
                        }
                    }
                } finally {
                    if (channelShell.getInputStream() != null) {
                        channelShell.getInputStream().close();
                    }
                }
            } finally {
                isExecutedFlag.set(true);
            }
            if (future.isDone()) {
                throw new SshExecutionTimeoutException(command, properties.getTimeoutExecute());
            } else {
                future.cancel(true);
            }
            log.debug("Exec took {} ms", System.currentTimeMillis() - start);
            log.trace("Output of {} execution:\n{}", ChannelType.SHELL, output);
        });
        return output.toString();
    }

    @NotNull
    private ScheduledFuture<?> interruptExecutionOnTimeout(@NotNull String command, AtomicBoolean isExecutedFlag,
                                                           Channel channelShell) {
        Map<String, String> mdcMap = MDC.getCopyOfContextMap();
        log.debug("Create SSH interrupt scheduler for channel {} with timeout {}", channelShell.getId(),
                properties.getTimeoutExecute());
        return executorService.schedule(() -> {
            Thread.currentThread().setName("mia_ssh_timeout_channel_" + channelShell.getId());
            MdcUtils.setContextMap(mdcMap);
            if (!isExecutedFlag.get()) {
                try {
                    channelShell.sendSignal(INTERRUPT_SIGNAL);
                    log.info("Ssh command execution was interrupted by timeout.\n"
                            + "command: [" + command + "], timeout: [" + properties.getTimeoutExecute() + " ms]");
                } catch (Exception e) {
                    log.info("Can't get channelShell output to interrupt timeout connection "
                            + "[chId = {}]", channelShell.getId());
                } finally {
                    log.info("Closing channel due timeout [chId = {}]", channelShell.getId());
                    channelShell.disconnect();
                    Thread.currentThread().interrupt();
                }
            }
        }, properties.getTimeoutExecute(), TimeUnit.MILLISECONDS);
    }

    /**
     * Uploads a file.
     */
    public void uploadFileOnServer(File file, String pathToUpLoad, String workingDirectory) {
        boolean isTmpPathForUpload = isPrefixPresent() || saveFilesToWorkingDir;
        final String tmpPathForUpload = isTmpPathForUpload ? workingDirectory : pathToUpLoad;
        log.info("Upload file {} to {}", file, tmpPathForUpload);
        String command = "mkdir -p " + tmpPathForUpload + "\nchmod 777 " + tmpPathForUpload;
        this.runCommand(updateCommandForExternalEnv(command));
        putFileFromServer(file.toPath(), tmpPathForUpload);
        if (isTmpPathForUpload) {
            this.runCommand(updateCommandForExternalEnv("chmod 777 " + workingDirectory));
            final String tmpPathToFile = FileUtils.tempFileName(workingDirectory, file.getName());
            transferFileOnServer(tmpPathToFile, pathToUpLoad);
        }
    }

    /**
     * Transfer a file.
     */
    public void transferFileOnServer(String pathToFile, String pathToUpLoad) {
        log.info("Transferring file from [ " + pathToFile + " ]" + " to [ " + pathToUpLoad + " ]");
        String command = "chmod 777 " + pathToFile + "\ncp -p " + pathToFile + " " + pathToUpLoad;
        runCommand(updateCommandForExternalEnv(command));
    }

    /**
     * Get file from ssh server.
     */
    public File getFileFromServer(String path, String workingDirectory) {
        log.info("Getting file {}", path);
        String tempPath = path;
        String fileName = FilenameUtils.getName(path);
        if (isPrefixPresent() || saveFilesToWorkingDir) {
            log.debug("moving file to working directory before downloading it, src: {}, dest: {}",
                    path, workingDirectory);
            try {
                transferFileOnServer(path, workingDirectory);
                tempPath = FileUtils.tempFileName(workingDirectory, fileName);
            } catch (Exception e) {
                log.error("Can't move file to working directory before download to MIA. "
                        + "workingDirectory: {}, filePath: {}", workingDirectory, path, e);
            }
        }
        final String src = tempPath;
        final File dest = miaContext.getLogPath().resolve(fileName).toFile();
        dest.getParentFile().mkdirs();
        commandToExecute = new StringBuilder(String.format("get file from server [src: %s, dest: %s]",
                src, dest.getAbsolutePath()));
        String exceptionStr = "Failed to " + commandToExecute;
        channelFlow(ChannelType.SFTP, true, exceptionStr,
                (channel) -> ((ChannelSftp) channel).get(src, dest.getPath()));
        log.info("Got file, newPath: {}; oldPath: {}", dest, path);
        return dest;
    }

    /**
     * Put file from ssh server.
     */
    public void putFileFromServer(Path pathToFile, String pathToUpLoad) {
        channelFlow(ChannelType.SFTP, true, String.format("Error while put file %s to %s", pathToFile, pathToUpLoad),
                (channel) -> {
                    final ChannelSftp sftpChannel = (ChannelSftp) channel;
                    sftpChannel.put(pathToFile.toString(), pathToUpLoad.trim());
                    final String pathToUploaded =
                            FileUtils.tempFileName(pathToUpLoad, pathToFile.getFileName().toString());
                    try {
                        sftpChannel.chmod(Integer.parseInt("777", 8), pathToUploaded);
                    } catch (SftpException e) {
                        if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                            // At Bulk Validator files are removed right after upload, it's fix.
                            log.error("Can't use chmod at uploaded file [{}], it doesn't exist anymore!",
                                    pathToUploaded);
                        } else {
                            throw e;
                        }
                    }
                });
    }

    /**
     * Remove file from ssh server.
     */
    public void removeFileFromServer(String pathToFile) {
        channelFlow(ChannelType.SFTP, true, "Error while remove file %s" + pathToFile,
                (channel) -> ((ChannelSftp) channel).rm(pathToFile));
    }

    /**
     * Flow for channel which creates ssh session and opens ssh channel.
     *
     * @param channelType      channelType
     * @param connect          need to connect?
     * @param exceptionString  string of exception in problem case
     * @param throwingConsumer consumer
     */
    private void channelFlow(ChannelType channelType, boolean connect, String exceptionString,
                             ThrowingConsumer<Exception> throwingConsumer) {
        log.trace("Run flow: ");
        long sessionStart = System.currentTimeMillis();
        Channel channel = null;
        int channelId = -2;
        int retryCount = 3;
        long retryTimeout = (properties.getTimeOutFileDownload()) <= 0 ? 500 : properties.getTimeOutFileDownload();
        for (int retryId = 0; retryId <= retryCount; retryId++) {
            try {
                log.trace("Open '{}' channel [{}]", channelType, managerInstanceNumber);
                channel = sshSession.openChannel(channelType);
                channelId = channel == null ? -1 : channel.getId();
                log.info("Channel '{}, {}' opened [user: {}, connManager:{}]",
                        channelType, channelId, properties, managerInstanceNumber);
                if (connect) {
                    channel.connect(properties.getTimeoutConnect());
                    log.trace("Channel '{}, {}' is connected in flow [{}]",
                            channelType, channelId, managerInstanceNumber);
                }
                throwingConsumer.accept(channel);
                retryId = 4;
            } catch (SshExecutionTimeoutException e) {
                log.debug("Ssh timeout in channel {} [{}]", channelId, managerInstanceNumber);
                throw e;
            } catch (SftpException e) {
                if (retryId >= retryCount) {
                    String noFileErr = e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE ? "(no such file) " : "";
                    throw new SshSftpException(noFileErr, exceptionString, channelId, managerInstanceNumber, e);
                } else {
                    log.warn("{} on trial #{}, retry again after {} ms", exceptionString, (retryId + 1), retryTimeout);
                    Utils.sleepForTimeInMillSeconds(retryTimeout);
                }
            } catch (JSchException e) {
                throw new SshException("Error during connection/open channel. "
                        + exceptionString, channelId, managerInstanceNumber, e);
            } catch (MiaException e) {
                throw e;
            } catch (Exception e) {
                throw new SshException(exceptionString, channelId, managerInstanceNumber, e);
            } finally {
                sshSession.closeChannel(channel);
                log.debug("SSH finished! Session length {} [channel_Id: {}, {}]",
                        System.currentTimeMillis() - sessionStart, channelId, managerInstanceNumber);
            }
        }
    }

    /**
     * Update command in case when external environment is used and pty is "false".
     * Need add & because when no command output shell can't receive any output.
     *
     * @param command command to execute
     * @return updated command to execute
     */
    private String updateCommandForExternalEnv(String command) {
        if (isPrefixPresent() && !properties.isPty()) {
            command += " &";
        }
        return command;
    }

    /**
     * Gets executed command.
     *
     * @return executed command
     */
    public String getExecutedCommand() {
        return commandToExecute == null ? "" : commandToExecute.toString();
    }

    private boolean isPrefixPresent() {
        return !Strings.isNullOrEmpty(externalPrefix);
    }

    /**
     * Gets connectionInfo.
     *
     * @return MAP with information about host and user.
     */
    public Map<String, String> connectionInfo() {
        final HashMap<String, String> connectionInfo = new HashMap<>();
        connectionInfo.put("host", properties.getHostname());
        connectionInfo.put("user", properties.getUsername());
        return connectionInfo;
    }

    @FunctionalInterface
    public interface ThrowingConsumer<E extends Exception> {

        void accept(Channel channel) throws E;
    }
}
