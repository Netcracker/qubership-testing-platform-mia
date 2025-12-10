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

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Properties;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.qubership.atp.mia.exceptions.businesslogic.ssh.SshChannelCreateFailException;
import org.qubership.atp.mia.exceptions.businesslogic.ssh.SshChannelCreateInterruptionException;
import org.qubership.atp.mia.exceptions.businesslogic.ssh.SshChannelsBusyException;
import org.qubership.atp.mia.exceptions.businesslogic.ssh.SshCreateSessionException;
import org.qubership.atp.mia.exceptions.businesslogic.ssh.SshCreateSessionFailException;
import org.qubership.atp.mia.exceptions.businesslogic.ssh.SshRsaAddFailedException;
import org.qubership.atp.mia.model.configuration.CommonConfiguration;
import org.qubership.atp.mia.model.environment.ConnectionProps;
import org.qubership.atp.mia.model.environment.Server;
import org.qubership.atp.mia.repo.impl.pool.ssh.ChannelType;
import org.qubership.atp.mia.repo.impl.pool.ssh.SshSessionPool;
import org.qubership.atp.mia.utils.CryptoUtils;
import org.springframework.lang.NonNull;
import org.springframework.retry.RetryException;

import com.google.common.base.Strings;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SshSession {

    private Session session;

    @Getter
    private final UUID sessionId = UUID.randomUUID();
    private final int retries = 2;
    private final AtomicInteger channelsCounter;
    private final AtomicInteger openChannels;
    private final ReentrantLock locker;
    private final Condition maximumChannels;
    private final JSch jsch;
    private final ConnectionProps properties;

    private String rsaFilePath;
    private boolean isIdentityAdded;

    /**
     * Creates details for ssh session, which used when opening {@link Channel} and {@link Session}.
     *
     * @param server        with credentials
     * @param configuration used to get SshRsaFilePath field of CommonConfiguration.
     */
    public SshSession(Server server, CommonConfiguration configuration) {
        jsch = getJsch(configuration);
        properties = ConnectionProps.forSsh(server);
        channelsCounter = new AtomicInteger();
        openChannels = new AtomicInteger();
        locker = new ReentrantLock();
        maximumChannels = locker.newCondition();
        log.trace("{} created and has environment properties: {}.", sessionId, properties.fullInfo());
    }

    /**
     * Sets necessary parameters of ssh connection(like ssh identity).
     * This method is required to use before execution of any ssh command.
     *
     * @param configuration - CommonConfiguration.
     */
    private JSch getJsch(CommonConfiguration configuration) {
        JSch jsch = new JSch();
        if (configuration != null && !Strings.isNullOrEmpty(configuration.getSshRsaFilePath())) {
            rsaFilePath = configuration.getSshRsaFilePath();
            try {
                jsch.addIdentity(configuration.getSshRsaFilePath());
                log.debug("RSA identity added");
            } catch (JSchException e) {
                throw new SshRsaAddFailedException(configuration.getSshRsaFilePath(), e);
            }
        }
        return jsch;
    }

    /**
     * Opens {@link Channel}, don't requires already opened {@link Session}, because does it itself.
     *
     * @param channelType type of channel depends on ssh operation, check {@link ChannelType}.
     * @return opened channel or throw error.
     */
    public Channel openChannel(ChannelType channelType) {
        locker.lock();
        try {
            if (!isConnected()) {
                createSession(1);
            }
            channelsCounter.incrementAndGet();
            return openChannel(channelType, 1);
        } finally {
            channelsCounter.decrementAndGet();
            locker.unlock();
        }
    }

    private Channel openChannel(ChannelType channelType, int retryCount) {
        Channel ch = null;
        UUID channelId = UUID.randomUUID();
        try {
            log.trace("Open channels for session {}: {}/{}", sessionId,
                    openChannels.get(), properties.getChannelsPerSession());
            int i = 0;
            int timeout = properties.getTimeoutExecute() / 1000;
            while (openChannels.get() >= properties.getChannelsPerSession()) {
                log.warn("Maximum channels({}) per sessions ({}) achieved. Waiting for resolve to open channel {}",
                        properties.getChannelsPerSession(), sessionId, channelId);
                if (!maximumChannels.await(new Random().nextInt(100) + 900, TimeUnit.MILLISECONDS)) {
                    log.error("Condition await failed for session with ID #{}", sessionId);
                    if (i++ > timeout) {
                        throw new SshChannelsBusyException(timeout);
                    }
                }
            }
            openChannels.incrementAndGet();
            try {
                log.trace("Free to open channel with ID {}", channelId);
                ch = session.openChannel(channelType.toString());
                if (ch == null || ch.isClosed()) {
                    String err = String.format("Can't open channel it %s", ch == null
                            ? "is null" : ch.isClosed()
                            ? "is closed" : "unknown reason");
                    throw new Exception(err);
                } else {
                    log.debug("Channel with ID {} opened successfully", channelId);
                }
            } catch (Exception e) {
                log.error("Can't open ssh channel with ID {}: {}", channelId, e.getMessage());
                if (retryCount++ >= retries) {
                    throw new SshChannelCreateFailException(channelId, e.getMessage());
                }
                log.debug("Retry open channel with ID {}", channelId);
                openChannel(channelType, retryCount);
            }
        } catch (InterruptedException e) {
            throw new SshChannelCreateInterruptionException(channelId, e.getMessage());
        }
        return ch;
    }

    /**
     * Close channel.
     *
     * @param channel channel
     */
    public void closeChannel(Channel channel) {
        log.debug("Close channel for session {}", sessionId);
        if (channel != null && channel.isConnected()) {
            channel.disconnect();
        }
        log.debug("Open channels for session {}: {}/{}", sessionId,
                openChannels.decrementAndGet(), properties.getChannelsPerSession());
    }

    /**
     * Creates jschSession.
     *
     * @param retryCount pass 1 to make retry amount equal to this.retries.
     */
    public void createSession(int retryCount) {
        if (!isConnected()) {
            log.trace("Create a new session with {} in sshManager №{}", properties, sessionId);
            try {
                addIdentity(jsch);
                session = jsch.getSession(properties.getUsername(), properties.getHostname(), properties.getPort());
                session.setServerAliveInterval(SshSessionPool.KEEP_ALIVE_MSG_INTERVAL);
                session.setConfig(getSessionConfig(session));
                session.setPassword(CryptoUtils.decryptValue(properties.getPassword()));
                session.setTimeout(properties.getTimeoutConnect());
                session.setServerAliveCountMax(Integer.MAX_VALUE);
                session.connect(properties.getTimeoutConnect());
                if (!session.isConnected()) {
                    throw new RetryException("Session is not connected after attempt.");
                }
                log.trace("Session created for {}", sessionId);
            } catch (RetryException e) {
                log.error("Session didn't open after connect {}. {}", sessionId, e.getMessage());
                if (retryCount++ >= retries) {
                    throw new SshCreateSessionFailException(properties, e.getMessage());
                }
                log.warn("Trying to reconnect the session {}", sessionId);
                createSession(retryCount);
            } catch (Exception e) {
                throw new SshCreateSessionException(properties.fullInfo(), e.getMessage());
            }
        }
    }

    private void addIdentity(JSch jsch) {
        if (!isIdentityAdded && !Strings.isNullOrEmpty(properties.getKey())) {
            final byte[] key = CryptoUtils.decryptValue(properties.getKey()).getBytes(StandardCharsets.UTF_8);
            final byte[] passphrase = Strings.isNullOrEmpty(properties.getPassphrase())
                    ? null
                    : CryptoUtils.decryptValue(properties.getPassphrase()).getBytes(StandardCharsets.UTF_8);
            try {
                jsch.addIdentity("id_rsa", key, null, passphrase);
                isIdentityAdded = true;
                log.debug("id_rsa identity added");
            } catch (JSchException e) {
                throw new SshRsaAddFailedException("properties", e);
            }
        }
    }

    /**
     * Gets Session config.
     *
     * @return session config
     */
    private Properties getSessionConfig(Session session) {
        return new Properties() {
            {
                put("StrictHostKeyChecking", "no");  // not recommended
                put("PreferredAuthentications", "publickey,keyboard-interactive,password");
                put("cipher.c2s", session.getConfig("cipher.c2s") + ",ssh-rsa,signature.dss");
                put("cipher.s2c", session.getConfig("cipher.s2c") + ",ssh-rsa,signature.dss");
                put("server_host_key", session.getConfig("server_host_key") + ",ssh-rsa,signature.dss");
                put("PubkeyAcceptedAlgorithms", session.getConfig("PubkeyAcceptedAlgorithms")
                        + ",ssh-rsa,signature.dss");
                if (properties.getSshServerKexAlgorithms() != null
                        && !properties.getSshServerKexAlgorithms().trim().isEmpty()) {
                    put("kex", properties.getSshServerKexAlgorithms());
                }
            }
        };
    }

    /**
     * Is session connected.
     *
     * @return true or false.
     */
    public boolean isConnected() {
        return session != null && session.isConnected();
    }

    /**
     * Properties with ssh information, such as host, port etc.
     *
     * @return ssh connection properties.
     */
    @NonNull
    public ConnectionProps getProperties() {
        return properties;
    }

    /**
     * Disconnects ssh session if it was connected.
     */
    public boolean disconnect() {
        log.debug("Trying to disconnect SSH connection [{}]", sessionId);
        boolean isDisconnected = false;
        if (isConnected()) {
            if (!isExecuting()) {
                session.disconnect();
                isDisconnected = true;
                log.info("Disconnect SSH connection {}, successfully [{}]", this.properties, sessionId);
            } else {
                log.debug("Session is executing.");
            }
        } else {
            log.debug("SSH connection null or just was disconnected, no action required.");
        }
        return isDisconnected;
    }

    public boolean isExecuting() {
        return channelsCounter.get() > 0 || openChannels.get() > 0;
    }

    /**
     * Checks that properties not changed since connection store.
     * It helps to avoid situations when connection is old, while configuration updated.
     *
     * @param server        server.
     * @param configuration only check sshRsaFilePath field of CommonConfiguration.
     * @return false if not the same.
     */
    public boolean isSame(Server server, CommonConfiguration configuration) {
        boolean propEq = Objects.equals(ConnectionProps.forSsh(server), properties);
        boolean rsaEq;
        if (configuration == null) {
            rsaEq = Strings.isNullOrEmpty(rsaFilePath);
        } else {
            if (Strings.isNullOrEmpty(configuration.getSshRsaFilePath())) {
                rsaEq = Strings.isNullOrEmpty(rsaFilePath);
            } else {
                rsaEq = !Strings.isNullOrEmpty(rsaFilePath);
            }
        }
        return propEq && rsaEq;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SshSession)) {
            return false;
        }
        SshSession that = (SshSession) o;
        return properties.equals(that.properties)
                && Objects.equals(isIdentityAdded, that.isIdentityAdded);
    }

    @Override
    public int hashCode() {
        return Objects.hash(properties, isIdentityAdded);
    }
}
