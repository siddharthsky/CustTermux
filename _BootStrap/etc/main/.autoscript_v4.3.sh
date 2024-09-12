#!/bin/bash

# Detect the shell
SHELL_NAME=$(basename "$SHELL")

case "$SHELL_NAME" in
    "bash"|"zsh"|"fish")
        # Supported shells: Bash, Zsh, Fish (no action needed)
        ;;
    *)
        echo "Unsupported shell: $SHELL_NAME"
        exit 1
        ;;
esac

################################################################################################
# Utility functions
################################################################################################

IP_ADD=""
get_ip_address() {
    local ip_address=$(termux-wifi-connectioninfo | grep -oP '(?<="ip": ")[^"]+')
    IP_ADD="$ip_address"
}

Server_Runner() {
    pkill -f "$HOME/.jiotv_go/bin/jiotv_go"
    pkill -f "jiotv_go"
}

TheShowRunner() {
    retrieve_first_line() {
        file="$1"
        if [ -f "$file" ]; then
            head -n 1 "$file"
        else
            echo ""
        fi
    }

    default_port=5001
    retrieved_port=$(retrieve_first_line "$HOME/.jiotv_go/bin/server_port.cfg")

    if [[ "$retrieved_port" =~ ^[0-9]{4}$ ]]; then
        port_to_use=$retrieved_port
    else
        file="$HOME/.jiotv_go/bin/server_port.cfg"
        touch "$file"
        chmod 755 "$file"
        echo "5001" > "$file"
        port_to_use=$default_port
    fi

    get_value_from_key "server_setup_isLocal" "VARIABLE03"

    if [ "$VARIABLE03" == "Yes" ]; then
        termux-wake-lock
        am start --user 0 -a com.termux.SKY_ACTION -n com.termux/.SkyActionActivity -e mode "loginstatus2" &
        $HOME/.jiotv_go/bin/jiotv_go run --port $port_to_use > /dev/null 2>&1 &
        echo -e "\e[32mRunning Server Locally on port $port_to_use\e[0m"
    else
        termux-wake-lock
        am start --user 0 -a com.termux.SKY_ACTION -n com.termux/.SkyActionActivity -e mode "loginstatus2" &
        $HOME/.jiotv_go/bin/jiotv_go run --port $port_to_use --public > /dev/null 2>&1 &
        echo -e "\e[32mRunning Server on port $port_to_use\e[0m"
    fi
}

TheShowRunner_onetime() {
    a_username=$(whoami)
    am start -a com.termux.SaveReceiver -n com.termux/.SkySharedPrefActivity --es key server_setup_username --es value $a_username

    get_value_from_key "server_setup_isLocal" "VARIABLE03"

    if [ "$VARIABLE03" == "Yes" ]; then
        echo -e "\e[32mRunning Server Locally\e[0m"
        $HOME/.jiotv_go/bin/jiotv_go run > /dev/null 2>&1 &
    else
        $HOME/.jiotv_go/bin/jiotv_go run -P > /dev/null 2>&1 &
    fi

    am start --user 0 -a com.termux.SKY_ACTION -n com.termux/.SkyActionActivity -e mode "loginstatus2"
}

################################################################################################
# AM functions
################################################################################################

get_value_from_key() {
    local KEY="$1"
    local VAR_NAME="$2"
    logcat -c
    sleep 0
    am start -a com.termux.GetReceiver -n com.termux/.SkySharedPrefActivity --es key "$KEY"
    sleep 0
    local VALUE=$(logcat -d | grep "SkySharedPrefActivity" | grep "$KEY" | awk -F'value: ' '{print $2}' | head -n 1)

    if [ -z "$VALUE" ]; then
        VALUE=$(logcat -d | grep "Retrieved key:" | grep "$KEY" | awk -F'value: ' '{print $2}' | head -n 1)
    fi

    eval "$VAR_NAME='$VALUE'"
    echo "Captured value: $(eval echo \$$VAR_NAME)"
}

################################################################################################
# Installation functions
################################################################################################

Setup_Prerequisites() {
    rm -f $HOME/.termux/termux.properties
    touch $HOME/.termux/termux.properties
    chmod 755 $HOME/.termux/termux.properties
    echo "allow-external-apps = true" >> $HOME/.termux/termux.properties

    FILE_URL="https://bit.ly/setpasswordexp"
    curl -SL --progress-bar --retry 2 --retry-delay 2 -o "$HOME/.set_password.exp" "$FILE_URL" || { echo "Failed to download binary"; exit 1; }
    chmod 755 "$HOME/.set_password.exp"
}

Default_Installation() {
    case "$OSTYPE" in
        "linux-android"*) OS="android" ;;
        "linux-"*) OS="linux" ;;
        "darwin"*) OS="darwin" ;;
        *) echo "Unsupported operating system: $OSTYPE"; exit 1 ;;
    esac

    echo "Step 1: Identified operating system as $OS"
    ARCH=$(uname -m)

    case $ARCH in
        "x86_64") ARCH="amd64" ;;
        "aarch64" | "arm64") ARCH="arm64" ;;
        "i386" | "i686") ARCH="386" ;;
        "arm"*) ARCH="arm" ;;
        *) echo "Unsupported architecture: $ARCH"; exit 1 ;;
    esac

    echo "Step 2: Identified processor architecture as $ARCH"
    mkdir -p "$HOME/.jiotv_go/bin"
    echo "Step 3: Created \$HOME/.jiotv_go/bin"

    if [ "$OS" = "android" ] && [ "$ARCH" = "arm" ]; then
        SDK_VERSION=$(getprop ro.build.version.sdk)
        if [ "$SDK_VERSION" -le 23 ]; then
            OS="android5"
            ARCH="armv7"
            pkg install termux-elf-cleaner -y
        fi
    fi

    BINARY_URL="https://github.com/rabilrbl/jiotv_go/releases/latest/download/jiotv_go-$OS-$ARCH"
    curl -SL --progress-bar --retry 2 --retry-delay 2 -o "$HOME/.jiotv_go/bin/jiotv_go" "$BINARY_URL" || { echo "Failed to download binary"; exit 1; }
    chmod 755 "$HOME/.jiotv_go/bin/jiotv_go"

    if [ "$OS" = "android" ] && [ "$ARCH" = "arm" ]; then
        SDK_VERSION=$(getprop ro.build.version.sdk)
        if [ "$SDK_VERSION" -le 23 ]; then
            termux-elf-cleaner "$HOME/.jiotv_go/bin/jiotv_go"
        fi
    fi

    echo "Step 5: Granted executable permissions to the binary"
    case "$SHELL_NAME" in
        "bash")
            export PATH="$PATH:$HOME/.jiotv_go/bin"
            echo "export PATH=$PATH:$HOME/.jiotv_go/bin" >> "$HOME/.bashrc"
            ;;
        "zsh")
            export PATH="$PATH:$HOME/.jiotv_go/bin"
            echo "export PATH=$PATH:$HOME/.zshrc"
            ;;
        "fish")
            echo "set -gx PATH $PATH $HOME/.jiotv_go/bin" >> "$HOME/.config/fish/config.fish"
            echo "Please restart your terminal or run source $HOME/.config/fish/config.fish"
            ;;
    esac
}

Setup_Extra() {
    echo "EPG UTIL: STARTED"
    $HOME/.jiotv_go/bin/jiotv_go epg gen
    echo "EPG UTIL: FINISHED"
    am startservice -n com.termux/.app.TermuxService -a com.termux.service_execute
}

################################################################################################
# Main logic
################################################################################################

SDK_VERSION=$(getprop ro.build.version.sdk)
if [ "$SDK_VERSION" -le 23 ]; then
    echo "Script: v6.9.5z [5 series]"
else
    echo "Script: v6.9.5z [7 series]"
fi

FILE_PATH="$HOME/.jiotv_go/bin/jiotv_go"

if [ ! -f "$FILE_PATH" ]; then
    while true; do
        get_value_from_key "isServerSetupDone" "VARIABLE00"
        if [ "$VARIABLE00" == "Done" ]; then
            echo "Initial setup is complete."
            break
        else
            echo "Waiting for initial setup to complete..."
            sleep 3
        fi
    done
    echo "-----------------------"
    echo "INSTALLATION -- PART 1"
    echo "-----------------------"
    mkdir -p "$HOME/.jiotv_go/bin/"
    Setup_Prerequisites
    clear
    echo "-----------------------"
    echo "INSTALLATION -- PART 2"
    echo "-----------------------"
    Default_Installation
    Setup_Extra
    clear
    Server_Runner
    TheShowRunner_onetime
else
    Server_Runner
    TheShowRunner
fi
