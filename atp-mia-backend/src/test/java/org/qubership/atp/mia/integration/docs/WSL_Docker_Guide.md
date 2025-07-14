
# Installation

## Enable WSL

> Open Power Shell console as administrator:

1. `dism.exe /online /enable-feature /featurename:Microsoft-Windows-Subsystem-Linux /all /norestart`
2. `dism.exe /online /enable-feature /featurename:VirtualMachinePlatform /all /norestart`
3. Reboot your PC
4. Download and install Linux kernel updates:  
   [wsl_update_x64.msi](https://wslstorestorage.blob.core.windows.net/wslblob/wsl_update_x64.msi)
5. Open PowerShell as administrator again:
   - `wsl --set-default-version 2`

---

## Install Ubuntu

> Open Power Shell console as administrator again:

```bash
wsl --install -d Ubuntu
```

If you see error `0x8024500c`, run:

```bash
wsl --update --web-download
wsl --install --web-download -d Ubuntu
```

---

# Docker

## Installation

> Open WSL console:

```bash
sudo apt-get update
sudo apt-get install apt-transport-https ca-certificates curl gnupg lsb-release
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg
echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt-get update
sudo apt-get install docker-ce docker-ce-cli containerd.io
sudo /etc/init.d/docker start
```

> Test Docker:

```bash
sudo docker run hello-world
```

If Docker daemon doesn't launch, try:

```bash
sudo dockerd
```

---

## Enable Docker Auto-Start in WSL

> ⚠️ After reboot or logout, WSL shuts down with Docker.

To start Docker automatically on WSL launch (based on [this StackOverflow answer](https://stackoverflow.com/questions/65813979)):

1. Open Ubuntu console
2. Add to `~/.profile`:
    ```bash
    echo 'sudo service docker status || sudo service docker start ' >> ~/.profile
    ```
3. Edit visudo:
    ```bash
    sudo visudo
    ```
4. Add the following line, replacing `username` with your actual Linux username:
    ```bash
    username ALL=(root) NOPASSWD: /usr/sbin/service docker *
    ```
5. Save (`Ctrl+S`, then `Ctrl+X`)

Now Docker will auto-start with Ubuntu.

To restart all containers after boot:

```bash
docker restart $(docker ps -a -q)
```

---

# Network Interaction

> In Docker Desktop you can access containers from LAN.  
> In WSL this requires manual setup.

### Steps

1. Find your WSL instance IP:
    ```bash
    sudo apt-get install net-tools
    ifconfig
    ```

2. Open PowerShell (Admin) and run:
    ```bash
    netsh interface portproxy add v4tov4 listenport=${YOUR_PORT} listenaddress=0.0.0.0 connectport=${YOUR_PORT} connectaddress=${WSL_INSTANCE_IP}
    ```

> Reference: [Microsoft Docs](https://docs.microsoft.com/en-us/windows/wsl/networking)

---

# Problems

## ❗ Error Initializing Network Controller

**Symptoms:**

```
failed to start daemon: Error initializing network controller: error obtaining controller instance: failed to create NAT chain DOCKER
iptables v1.8.x (legacy): can't initialize iptables table `nat': Table does not exist
(exit status 3)
```

> You are using **WSL1**

### ✅ Solution: Upgrade to WSL2

1. Check version:
    ```bash
    wsl --list --verbose
    ```

    You should see:
    ```
    NAME      STATE           VERSION
    * Ubuntu    Running         1
    ```

2. Reinstall and switch to WSL2:

    - Open Power Shell (admin):
      ```bash
      dism.exe /online /enable-feature /featurename:Microsoft-Windows-Subsystem-Linux /all /norestart
      ```
    - Reboot
    - [Download kernel update](https://wslstorestorage.blob.core.windows.net/wslblob/wsl_update_x64.msi)
    - Set WSL2 default:
      ```bash
      wsl --set-default-version 2
      ```
    - Unregister old Ubuntu:
      ```bash
      wsl --unregister Ubuntu
      ```
    - Reinstall Ubuntu, set up new user
    - Reinstall Docker

### 🛑 Using legacy IPTABLES *(Not recommended)*

---

# No Internet Connection

### Step 1: Diagnose

```bash
ping 8.8.8.8   # If fails – network problem  
ping google.com  # If fails – DNS problem
```

> [Cisco AnyConnect and WSL2 Issues](https://community.cisco.com/t5/vpn/anyconnect-wsl-2-windows-substem-for-linux/td-p/4179888)

---

## VPN: No connection to NC intranet

> Add NC DNS servers to `/etc/resolv.conf`

1. Backup and replace:
    ```bash
    sudo cp /etc/resolv.conf /etc/resolv.conf.new
    sudo unlink /etc/resolv.conf
    sudo mv /etc/resolv.conf.new /etc/resolv.conf
    ```

2. Make file editable:
    ```bash
    sudo chattr -i /etc/resolv.conf
    sudo nano /etc/resolv.conf
    ```

3. Add nameservers:
    ```
    nameserver 127.0.0.1
    nameserver 127.0.0.2
    nameserver 127.0.0.3
    nameserver 127.0.0.4
    nameserver 8.8.8.8
    ```

4. Lock file:
    ```bash
    sudo chattr +i /etc/resolv.conf
    ```

> [Official Troubleshooting Guide](https://learn.microsoft.com/en-us/windows/wsl/troubleshooting)

---

## VirtualBox Conflict

If using **VirtualBox**, WSL may fail with Cisco VPN.

### Solution:

1. Uninstall VirtualBox
2. Uninstall Cisco AnyConnect
3. Reinstall Cisco AnyConnect
4. Reboot PC
