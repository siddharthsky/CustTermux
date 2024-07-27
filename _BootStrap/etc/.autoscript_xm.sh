#!/bin/bash

# Detect the shell
SHELL_NAME=$(basename "$SHELL")

case "$SHELL_NAME" in
    "bash")
        # Bash shell detected (no action needed)
        ;;
    "zsh")
        # Zsh shell detected (no action needed)
        ;;
    "fish")
        # Fish shell detected (no action needed)
        ;;
    *)
        echo "Unsupported shell: $SHELL_NAME"
        exit 1
        ;;
esac

################################################################################################
# Utility functions
################################################################################################

wait_and_count() {
    local start_time=$(date +%s)
    local counter=0
    local spinner="/-\|"
    local bar_length=40

    echo "[${1}] Processing..."
    while true; do
        local current_time=$(date +%s)
        local elapsed_time=$((current_time - start_time))
        if [ $elapsed_time -gt $1 ]; then
            break
        fi

        local progress=$((elapsed_time * bar_length / $1))
        printf "\r[\033[0;32m%-*s\033[0m] %d%% %c" $bar_length $(printf '#%.0s' $(seq 1 $progress)) $((elapsed_time * 100 / $1)) ${spinner:counter%4:1}
        sleep 0.1
        counter=$((counter + 1))
    done
    printf "\r[\033[0;32m%-*s\033[0m] 100%% \n" $bar_length $(printf '#%.0s' $(seq 1 $bar_length))
}

IP_ADD=""
get_ip_address() {
    local ip_address=$(termux-wifi-connectioninfo | grep -oP '(?<="ip": ")[^"]+')
    IP_ADD="$ip_address"
}

retrieve_first_line() {
    local file_path=$1
    local option=""
    if [ -f "$file_path" ]; then
        option=$(head -n 1 "$file_path")
    else
        echo "$file_path file not found or empty."
    fi
    echo "$option"
}

Init_Server_Check() {
    pkill -f "$HOME/.jiotv_go/bin/jiotv_go"
    starter=$($HOME/.jiotv_go/bin/jiotv_go bg run) # For Login Checker
}

Init_Server_Check_Regular() {
    termux-wake-lock
    # pkill -f "$HOME/.jiotv_go/bin/jiotv_go"
}

Server_Runner() {
    get_ip_address
    $HOME/.jiotv_go/bin/jiotv_go -v
    echo "---------------------------"
    echo -e "\e[96mFor Local Access:\e[0m"
    echo -e "\e[96mLogin Page:\e[0m http://localhost:5001"
    echo -e "\e[96mIPTV Playlist:\e[0m http://localhost:5001/playlist.m3u"
    echo "---------------------------"
    echo -e "\e[93mFor External Access:\e[0m"
    echo -e "\e[93mLogin Page:\e[0m http://$IP_ADD:5001"
    echo -e "\e[93mIPTV Playlist:\e[0m http://$IP_ADD:5001/playlist.m3u"
    echo "---------------------------"

    source ~/.bashrc # PATH update

    TheShowRunner
}

# Checking required packages
gui_req() {
    pkg install termux-am jq termux-api -y
    rm -f $HOME/.termux/termux.properties
    touch $HOME/.termux/termux.properties
    chmod 755 $HOME/.termux/termux.properties
    echo "allow-external-apps = true" >> $HOME/.termux/termux.properties
}

check_termux_api() {
    app_permission_check() {
        mkdir -p "$HOME/.jiotv_go/bin/"
        touch "$HOME/.jiotv_go/bin/permission.cfg"
        quick_var=$(head -n 1 "$HOME/.jiotv_go/bin/permission.cfg")
        if [ "$quick_var" = "OVERLAY=TRUE" ]; then
            :
        else
            am start --user 0 -a android.settings.MANAGE_UNKNOWN_APP_SOURCES -d "package:com.termux"
            echo "waiting for app install permissions"
            wait_and_count 20
            echo "OVERLAY=TRUE" > "$HOME/.jiotv_go/bin/permission.cfg"
        fi
    }

    check_package() {
        PACKAGE_NAME="com.termux.api"
        out="$(pm path $PACKAGE_NAME --user 0 2>&1 </dev/null)"
        
        if [[ "$out" == *"$PACKAGE_NAME"* ]]; then
            echo -e "The package \e[32m$PACKAGE_NAME\e[0m is available."
            am start --user 0 -n com.termux/com.termux.app.TermuxActivity
            echo "If stuck, Please clear app data and restart your device."
            return 0
        else
            return 1
        fi
    }

    while ! check_package; do
        echo "The package $PACKAGE_NAME is not installed. Checking again..."
        curl -L -o "$HOME/Tapi.apk" "https://github.com/termux/termux-api/releases/download/v0.50.1/termux-api_v0.50.1+github-debug.apk"
        chmod 755 "$HOME/Tapi.apk"
        termux-open "$HOME/Tapi.apk"
        wait_and_count 20
    done
}

# Default Installation
Default_Installation() {
    OS=""
    case "$OSTYPE" in
        "linux-android"*)
            OS="android"
            ;;
        "linux-"*)
            OS="linux"
            ;;
        "darwin"*)
            OS="darwin"
            ;;
        *)
            echo "Unsupported operating system: $OSTYPE"
            exit 1
            ;;
    esac

    echo "Step 1: Identified operating system as $OS"
    ARCH=$(uname -m)

    case $ARCH in
        "x86_64")
            ARCH="amd64"
            ;;
        "aarch64" | "arm64")
            ARCH="arm64"
            ;;
        "i386" | "i686")
            ARCH="386"
            ;;
        "arm"*)
            ARCH="arm"
            ;;
        *)
            echo "Unsupported architecture: $ARCH"
            exit 1
            ;;
    esac

    echo "Step 2: Identified processor architecture as $ARCH"

    # Create necessary directories
    mkdir -p "$HOME/.jiotv_go/bin"
    echo "Step 3: Created \$HOME/.jiotv_go/bin"

    if [ "$OS" = "android" ] && [ "$ARCH" = "386" ]; then
        OS="linux"
    fi

    # Set binary URL
    BINARY_URL="https://github.com/rabilrbl/jiotv_go/releases/latest/download/jiotv_go-$OS-$ARCH"

    # Download the binary
    curl -SL --progress-bar --retry 2 --retry-delay 2 -o "$HOME/.jiotv_go/bin/jiotv_go" "$BINARY_URL" || { echo "Failed to download binary"; exit 1; }

    echo "Step 4: Fetch the latest binary"

    # Make the binary executable
    chmod +x "$HOME/.jiotv_go/bin/jiotv_go"
    echo "Step 5: Granted executable permissions to the binary"

    # Add binary to PATH
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
        *)
            echo "Unsupported shell: $SHELL_NAME"
            exit 1
            ;;
    esac
}

FINAL_INSTALL() {
    #send_otp
    #verify_otp
    #pkill -f "$HOME/.jiotv_go/bin/jiotv_go"
    $HOME/.jiotv_go/bin/jiotv_go epg gen
    echo "Running : \$HOME/.jiotv_go/bin/jiotv_go run -P"
}

# Check if jiotv_go exists
if [[ -f "$HOME/.jiotv_go/bin/jiotv_go" ]]; then
    Server_Runner
fi

echo "Script : version 6.4"

FILE_PATH="$HOME/.jiotv_go/bin/run_check.cfg"

if [ ! -f "$FILE_PATH" ]; then
    mkdir -p "$HOME/.jiotv_go/bin/"
    echo "FIRST_RUN" > "$FILE_PATH"
    echo "-----------------------"
    echo "INSTALLATION -- PART 1"
    echo "-----------------------"
    gui_req
    echo "SECOND_RUN" > "$FILE_PATH"
else
    RUN_STATUS=$(cat "$FILE_PATH")

    case "$RUN_STATUS" in
        "FIRST_RUN")
            echo "-----------------------"
            echo "INSTALLATION -- PART 1"
            echo "-----------------------"
            gui_req
			Default_Installation
            FINAL_INSTALL
			Server_Runner
            echo "SECOND_RUN" > "$FILE_PATH"
            ;;
        "SECOND_RUN")
            echo "-----------------------"
            echo "INSTALLATION -- PART 2"
            echo "-----------------------"
            Default_Installation
            FINAL_INSTALL
            echo "FINAL_RUN" > "$FILE_PATH"
            Server_Runner
            echo -e "----------------------------"
            echo -e "\e[0;36mCustTermux by SiddharthSky\e[0m"
            echo -e "----------------------------"
            ;;
        "FINAL_RUN")
            echo ""
            ;;
        *)
            echo "Something Went Wrong : Clear App Data"
            sleep 30
            exit 1
            ;;
    esac
fi
