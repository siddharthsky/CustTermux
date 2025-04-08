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
# Update bash.bashrc function
################################################################################################

# Bash_Bashrc() {
#     echo "Starting Bash_Bashrc function..."

#     # Check if $HOME/.v4.4.cfg exists
#     if [[ -f "$HOME/.v4.4.cfg" ]]; then
#         if ! grep -q ".autoscript_v4.4.sh" "$PREFIX/etc/bash.bashrc"; then
#             echo "[INFO] Installing v4.4..."
#             sed -i 's|URL3=".*"|URL3="https://raw.githubusercontent.com/siddharthsky/CustTermux/main/_BootStrap/etc/main/.autoscript_v4.4.sh"|' "$PREFIX/etc/bash.bashrc"
#             echo "[SUCCESS] Updated to v4.4"
#         fi
#     fi
    
#     echo "Exiting Bash_Bashrc function."
# }



# Bash_Bashrc

################################################################################################
# Utility functions
################################################################################################

Server_Runner() {
    $HOME/.jiotv_go/bin/jiotv_go -v # temp off
    echo "---------------------------"
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
        touch "$file" && chmod 600 "$file"
        echo "5001" > "$file"
        port_to_use=$default_port
    fi

    FILE_PATHX="$HOME/.jiotv_go/bin/drm/on.drm"

    if [ -f "$FILE_PATHX" ]; then
        export JIOTV_DRM=true
        echo "DRM is enabled [$JIOTV_DRM]"
        
    else
        export JIOTV_DRM=false
        echo "DRM is disabled [$JIOTV_DRM]"
    fi

    get_value_from_key "server_setup_isLocal" "VARIABLE03"

    get_value_from_key "server_setup_EX_done" "VARIABLE04"

    get_value_from_key "server_setup_isTATA" "VARIABLE05"


    if [ "$VARIABLE03" == "Yes" ]; then
        termux-wake-lock
        $HOME/.jiotv_go/bin/jiotv_go run --port $port_to_use > /dev/null 2>&1 &
        echo -e "\e[32mRunning JioTV GO Server Locally on port $port_to_use\e[0m"
    else
        termux-wake-lock
        $HOME/.jiotv_go/bin/jiotv_go run --port $port_to_use --public > /dev/null 2>&1 &
        echo -e "\e[32mRunning JioTV GO Server on port $port_to_use\e[0m"
    fi

     if [ "$VARIABLE05" == "Yes" ]; then
        echo -e "\e[32mStarting TATA PLAY PHP server on port 5353\e[0m"
        cd $HOME/tataON
        php -S 0.0.0.0:5353 > /dev/null 2>&1 &
    fi

    if [ "$VARIABLE04" == "Yes" ]; then
        echo -e "\e[32mStarting ZEE5 PHP server on port 5349\e[0m"
        cd $HOME/zeeON
        php -S 0.0.0.0:5349 > /dev/null 2>&1 &
    fi


        

    # Wait for the server to be live, with a maximum of 5 tries
    attempts=0
    max_attempts=5
    while [ $attempts -lt $max_attempts ]; do
        if curl -s http://localhost:$port_to_use > /dev/null; then
            # Start the activity when the server is live
            am start --user 0 -a com.termux.SKY_ACTION -n com.termux/.SkyActionActivity -e mode "loginstatus2" &
            break
        else
            echo "Attempt $((attempts + 1))/$max_attempts: Waiting for server to be live..."
            sleep 5
            attempts=$((attempts + 1))
        fi
    done

    if [ $attempts -ge $max_attempts ]; then
        echo -e "\e[31mServer did not become live after $max_attempts attempts\e[0m"
    fi
}

TheShowRunner_onetime() {
    a_username=$(whoami)
    am start -a com.termux.SaveReceiver -n com.termux/.SkySharedPrefActivity --es key server_setup_username --es value $a_username

    FILE_PATHX="$HOME/.jiotv_go/bin/drm/on.drm"

    if [ -f "$FILE_PATHX" ]; then
        export JIOTV_DRM=true
        echo "DRM is enabled [$JIOTV_DRM]"
        
    else
        export JIOTV_DRM=false
        echo "DRM is disabled [$JIOTV_DRM]"
    fi

    get_value_from_key "server_setup_isLocal" "VARIABLE03"

    if [ "$VARIABLE03" == "Yes" ]; then
        echo -e "\e[32mRunning Server Locally\e[0m"
        $HOME/.jiotv_go/bin/jiotv_go run > /dev/null 2>&1 &
    else
        echo -e "\e[32mRunning Server Publicly\e[0m"
        $HOME/.jiotv_go/bin/jiotv_go run -P > /dev/null 2>&1 &
    fi

    # Wait for the server to be live, with a maximum of 5 tries
    attempts=0
    max_attempts=5
    while [ $attempts -lt $max_attempts ]; do
        if curl -s http://localhost:$port_to_use > /dev/null; then
            # Start the activity when the server is live
            am start --user 0 -a com.termux.SKY_ACTION -n com.termux/.SkyActionActivity -e mode "loginstatus2"
            break
        else
            echo "Attempt $((attempts + 1))/$max_attempts: Waiting for server to be live..."
            sleep 5
            attempts=$((attempts + 1))
        fi
    done

    if [ $attempts -ge $max_attempts ]; then
        echo -e "\e[31mServer did not become live after $max_attempts attempts\e[0m"
    fi
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

    FILE_URL="https://raw.githubusercontent.com/siddharthsky/CustTermux/main/_BootStrap/etc/.set_password.exp"
    echo "Setting password file"
    sleep 3
    #FILE_URL="https://bit.ly/setpasswordexp" #^redirects here
    curl -SL --progress-bar --retry 2 --retry-delay 2 -o "$HOME/.set_password.exp" "$FILE_URL" || { echo "Failed to download binary"; exit 1; }
    chmod 755 "$HOME/.set_password.exp"
    
    if [ "$SDK_VERSION" -le 23 ]; then
        chmod 400 $PREFIX/libexec/termux-am/am.apk
     fi

}

Setup_Postrequisites() {

    FILE_URL="https://raw.githubusercontent.com/siddharthsky/CustTermux/main/_BootStrap/etc/.set_tls.exp"
    echo "Setting tls files"
    sleep 3
    curl -SL --progress-bar --retry 2 --retry-delay 2 -o "$HOME/.set_tls.exp" "$FILE_URL" || { echo "Failed to download binary"; exit 1; }
    chmod 755 "$HOME/.set_tls.exp"

    FILE_URL="https://raw.githubusercontent.com/siddharthsky/CustTermux/main/_BootStrap/etc/config.json"
    echo "Setting config file"
    sleep 3
    curl -SL --progress-bar --retry 2 --retry-delay 2 -o "$HOME/.jiotv_go/bin/config.json" "$FILE_URL" || { echo "Failed to download binary"; exit 1; }
    chmod 755 "$HOME/.jiotv_go/bin/config.json"

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

    echo "Step 5: Granted executable permissions to the binary"
    chmod 755 "$HOME/.jiotv_go/bin/jiotv_go"

    if [ "$OS" = "android5" ] && [ "$ARCH" = "armv7" ]; then
        echo "Step 6: Removing DT_FLAGS"
        pkill -f "$HOME/.jiotv_go/bin/jiotv_go"
        termux-elf-cleaner "$HOME/.jiotv_go/bin/jiotv_go"
    fi

   
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
    echo "Script: v7.01.5k [5 series]"
else
    echo "Script: v7.01.7k [7 series]"
fi

FILE_PATH="$HOME/.jiotv_go/bin/jiotv_go"

if [ ! -f "$FILE_PATH" ]; then
    counter=0  # Initialize counter
    while true; do
        get_value_from_key "isServerSetupDone" "VARIABLE00"
    
        if [ "$VARIABLE00" == "Done" ]; then
            echo "Initial setup is complete."
            break
        else
            echo "Waiting for initial setup to complete..."
    
            counter=$((counter + 1))  # Increment counter
    
            if [ $counter -eq 3 ]; then
                echo "Running 'am' fix..."
                chmod 400 $PREFIX/libexec/termux-am/am.apk  # Call 
            fi
    
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
    Setup_Postrequisites
    Setup_Extra
    clear
    Server_Runner
    TheShowRunner_onetime
else
    Server_Runner
    TheShowRunner
fi
