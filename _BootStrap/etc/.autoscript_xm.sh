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

IP_ADD=""
get_ip_address() {
    local ip_address=$(termux-wifi-connectioninfo | grep -oP '(?<="ip": ")[^"]+')
    IP_ADD="$ip_address"
}


Server_Runner() {
	#get_ip_address
	$HOME/.jiotv_go/bin/jiotv_go -v
	echo "---------------------------"
	echo -e "\e[96mFor Access:\e[0m"
	echo -e "\e[96mLogin Page:\e[0m http://localhost:5001"
	echo -e "\e[96mIPTV Playlist:\e[0m http://localhost:5001/playlist.m3u"
	echo "---------------------------"
	#source ~/.bashrc # PATH update
	pkill -f "$HOME/.jiotv_go/bin/jiotv_go"
	pkill -f "jiotv_go"
}

TheShowRunner() {

	#get_value_from_key_n1 "app_name"
	
	# # Check if the app name is "null"
	# if [ "$VARIABLE01" == "null" ]; then
	# 	get_value_from_key_n3 "server_setup_isLocal"
	# elif [ "$VARIABLE01" == "sky_web_tv" ]; then
	# 	get_value_from_key_n3 "server_setup_isLocal"
	# 	am start --user 0 -n com.termux/.WebPlayerActivity
	# else	
	# 	get_value_from_key_n2 "app_launchactivity"
	# 	get_value_from_key_n3 "server_setup_isLocal"
	# 	am start --user 0 -n "$VARIABLE01/$VARIABLE02"
	# fi

	get_value_from_key_n3 "server_setup_isLocal"
 
	if [ "$VARIABLE03" == "Yes" ]; then
 		echo -e "\e[32mRunning Server Locally\e[0m"
		$HOME/.jiotv_go/bin/jiotv_go bg run
	else
		$HOME/.jiotv_go/bin/jiotv_go bg run -a -P
	fi

	am start --user 0 -a com.termux.SKY_ACTION -n com.termux/.SkyActionActivity -e mode "loginstatus2"

  
}

TheShowRunner_onetime() {
	get_value_from_key_n3 "server_setup_isLocal"
 
	if [ "$VARIABLE03" == "Yes" ]; then
 		echo -e "\e[32mRunning Server Locally\e[0m"
		$HOME/.jiotv_go/bin/jiotv_go bg run
	else
		$HOME/.jiotv_go/bin/jiotv_go bg run -a -P
	fi

 	#am start --user 0 -a com.termux.SKY_ACTION -n com.termux/.SkyActionActivity -e mode "loginstatus2"
}


################################################################################################
# AM functions
################################################################################################

get_value_from_key_n1() {
    local KEY="$1"
    logcat -c
	sleep 0
	am start -a com.termux.GetReceiver -n com.termux/.SkySharedPrefActivity --es key "$KEY"
	sleep 0
	local VALUE=$(logcat -d | grep "SkySharedPrefActivity" | grep "$KEY" | awk -F'value: ' '{print $2}' | head -n 1)
	VARIABLE01=$VALUE
	#Debug
	echo "Captured value: $VARIABLE01"
}

get_value_from_key_n2() {
    local KEY="$1"
    logcat -c
	sleep 0
	am start -a com.termux.GetReceiver -n com.termux/.SkySharedPrefActivity --es key "$KEY"
	sleep 0
	local VALUE=$(logcat -d | grep "SkySharedPrefActivity" | grep "$KEY" | awk -F'value: ' '{print $2}' | head -n 1)
	VARIABLE02=$VALUE
	#Debug
	echo "Captured value: $VARIABLE02"
}

get_value_from_key_n3() {
    local KEY="$1"
    logcat -c
	sleep 0
	am start -a com.termux.GetReceiver -n com.termux/.SkySharedPrefActivity --es key "$KEY"
	sleep 0
	local VALUE=$(logcat -d | grep "SkySharedPrefActivity" | grep "$KEY" | awk -F'value: ' '{print $2}' | head -n 1)
	VARIABLE03=$VALUE
	#Debug
	echo "Captured value: $VARIABLE03"
}


################################################################################################
# Installation functions
################################################################################################


# Checking required packages
Setup_Prerequisites() {
    #pkg install termux-am jq termux-api -y
    rm -f $HOME/.termux/termux.properties
    touch $HOME/.termux/termux.properties
    chmod 755 $HOME/.termux/termux.properties
    echo "allow-external-apps = true" >> $HOME/.termux/termux.properties
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
    chmod 755 "$HOME/.jiotv_go/bin/jiotv_go"
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

Setup_Extra() {
    #$HOME/.jiotv_go/bin/jiotv_go epg gen
    am startservice -n com.termux/.app.TermuxService -a com.termux.service_execute
}

echo "Script : version 6.9"

FILE_PATH="$HOME/.jiotv_go/bin/jiotv_go"

if [ ! -f "$FILE_PATH" ]; then
	mkdir -p "$HOME/.jiotv_go/bin/"
	echo "-----------------------"
	echo "INSTALLATION -- PART 1"
	echo "-----------------------"
	Setup_Prerequisites
	Default_Installation
	Setup_Extra
 	clear
	Server_Runner
 	TheShowRunner_onetime
else
	Server_Runner
 	TheShowRunner
fi
