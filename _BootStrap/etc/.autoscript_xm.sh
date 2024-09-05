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
	# echo -e "\e[96mFor Access:\e[0m"
	# echo -e "\e[96mLogin Page:\e[0m http://localhost:5001"
	# echo -e "\e[96mIPTV Playlist:\e[0m http://localhost:5001/playlist.m3u"
	# echo "---------------------------"
	source ~/.bashrc # PATH update
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

    # Retrieve the port from the file
    retrieved_port=$(retrieve_first_line "$HOME/.jiotv_go/bin/server_port.cfg")

    # Validate if retrieved_port is a 4-digit number
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
        echo -e "\e[32mRunning Server Locally on port $port_to_use\e[0m"
        termux-wake-lock
        $HOME/.jiotv_go/bin/jiotv_go bg run
    else
        echo -e "\e[32mRunning Server on port $port_to_use\e[0m"
        termux-wake-lock
        $HOME/.jiotv_go/bin/jiotv_go bg run --args "--port $port_to_use --public"
    fi

    am start --user 0 -a com.termux.SKY_ACTION -n com.termux/.SkyActionActivity -e mode "loginstatus2"
}



TheShowRunner_onetime() {
	a_username=$(whoami)

	am start -a com.termux.SaveReceiver -n com.termux/.SkySharedPrefActivity --es key server_setup_username --es value $a_username

	get_value_from_key "server_setup_isLocal" "VARIABLE03"
 
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

# get_value_from_key_n0() {
#     local KEY="$1"
#     logcat -c
# 	sleep 0
# 	am start -a com.termux.GetReceiver -n com.termux/.SkySharedPrefActivity --es key "$KEY"
# 	sleep 0
# 	local VALUE=$(logcat -d | grep "SkySharedPrefActivity" | grep "$KEY" | awk -F'value: ' '{print $2}' | head -n 1)
# 	VARIABLE00=$VALUE
# 	#Debug
# 	echo "Captured value: $VARIABLE00"
# }

# get_value_from_key_n1() {
#     local KEY="$1"
#     logcat -c
# 	sleep 0
# 	am start -a com.termux.GetReceiver -n com.termux/.SkySharedPrefActivity --es key "$KEY"
# 	sleep 0
# 	local VALUE=$(logcat -d | grep "SkySharedPrefActivity" | grep "$KEY" | awk -F'value: ' '{print $2}' | head -n 1)
# 	VARIABLE01=$VALUE
# 	#Debug
# 	echo "Captured value: $VARIABLE01"
# }

# get_value_from_key_n2() {
#     local KEY="$1"
#     logcat -c
# 	sleep 0
# 	am start -a com.termux.GetReceiver -n com.termux/.SkySharedPrefActivity --es key "$KEY"
# 	sleep 0
# 	local VALUE=$(logcat -d | grep "SkySharedPrefActivity" | grep "$KEY" | awk -F'value: ' '{print $2}' | head -n 1)
# 	VARIABLE02=$VALUE
# 	#Debug
# 	echo "Captured value: $VARIABLE02"
# }

# get_value_from_key_n3() {
#     local KEY="$1"
#     logcat -c
# 	sleep 0
# 	am start -a com.termux.GetReceiver -n com.termux/.SkySharedPrefActivity --es key "$KEY"
# 	sleep 0
# 	local VALUE=$(logcat -d | grep "SkySharedPrefActivity" | grep "$KEY" | awk -F'value: ' '{print $2}' | head -n 1)
# 	VARIABLE03=$VALUE
# 	#Debug
# 	echo "Captured value: $VARIABLE03"
# }

#################################################################################################################
# get_value_from_key_n0() {
#     local KEY="$1"
#     logcat -c
#     sleep 0
#     am start -a com.termux.GetReceiver -n com.termux/.SkySharedPrefActivity --es key "$KEY"
#     sleep 0
#     local VALUE=$(logcat -d | grep "SkySharedPrefActivity" | grep "$KEY" | awk -F'value: ' '{print $2}' | head -n 1)
    
#     if [ -z "$VALUE" ]; then
#         VALUE=$(logcat -d | grep "Retrieved key:" | grep "$KEY" | awk -F'value: ' '{print $2}' | head -n 1)
#     fi
    
#     VARIABLE00=$VALUE
#     # Debug
#     echo "Captured value: $VARIABLE00"
# }


# get_value_from_key_n1() {
#     local KEY="$1"
#     logcat -c
#     sleep 0
#     am start -a com.termux.GetReceiver -n com.termux/.SkySharedPrefActivity --es key "$KEY"
#     sleep 0
#     local VALUE=$(logcat -d | grep "SkySharedPrefActivity" | grep "$KEY" | awk -F'value: ' '{print $2}' | head -n 1)
    
#     if [ -z "$VALUE" ]; then
#         VALUE=$(logcat -d | grep "Retrieved key:" | grep "$KEY" | awk -F'value: ' '{print $2}' | head -n 1)
#     fi
    
#     VARIABLE01=$VALUE
#     # Debug
#     echo "Captured value: $VARIABLE01"
# }

# get_value_from_key_n2() {
#     local KEY="$1"
#     logcat -c
#     sleep 0
#     am start -a com.termux.GetReceiver -n com.termux/.SkySharedPrefActivity --es key "$KEY"
#     sleep 0
#     local VALUE=$(logcat -d | grep "SkySharedPrefActivity" | grep "$KEY" | awk -F'value: ' '{print $2}' | head -n 1)
    
#     if [ -z "$VALUE" ]; then
#         VALUE=$(logcat -d | grep "Retrieved key:" | grep "$KEY" | awk -F'value: ' '{print $2}' | head -n 1)
#     fi
    
#     VARIABLE02=$VALUE
#     # Debug
#     echo "Captured value: $VARIABLE02"
# }

# get_value_from_key_n3() {
#     local KEY="$1"
#     logcat -c
#     sleep 0
#     am start -a com.termux.GetReceiver -n com.termux/.SkySharedPrefActivity --es key "$KEY"
#     sleep 0
#     local VALUE=$(logcat -d | grep "SkySharedPrefActivity" | grep "$KEY" | awk -F'value: ' '{print $2}' | head -n 1)
    
#     if [ -z "$VALUE" ]; then
#         VALUE=$(logcat -d | grep "Retrieved key:" | grep "$KEY" | awk -F'value: ' '{print $2}' | head -n 1)
#     fi
    
#     VARIABLE03=$VALUE
#     # Debug
#     echo "Captured value: $VARIABLE03"
# }

#################################################################################################################

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
    # Debug
    echo "Captured value: $(eval echo \$$VAR_NAME)"    
}





################################################################################################
# Installation functions
################################################################################################


# Checking required packages
Setup_Prerequisites() {
    #pkg install termux-am jq termux-api -y
    pkg install termux-elf-cleaner -y
    rm -f $HOME/.termux/termux.properties
    touch $HOME/.termux/termux.properties
    chmod 755 $HOME/.termux/termux.properties
    echo "allow-external-apps = true" >> $HOME/.termux/termux.properties

    #FILE_URL="https://raw.githubusercontent.com/siddharthsky/CustTermux-JioTVGo/main/_BootStrap/etc/.set_password.exp"
    FILE_URL="https://bit.ly/setpasswordexp" #^redirects here
    curl -SL --progress-bar --retry 2 --retry-delay 2 -o "$HOME/.set_password.exp" "$FILE_URL" || { echo "Failed to download binary"; exit 1; }
    chmod 755 "$HOME/.set_password.exp"
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

	if [ "$OS" = "android" ] && [ "$ARCH" = "arm" ]; then
	  # Get the SDK version
	  SDK_VERSION=$(getprop ro.build.version.sdk)
	
	  # Check if the SDK version is equal to or less than 23
	  if [ "$SDK_VERSION" -le 23 ]; then
	    # Set OS to "android5"
	    OS="android5"
     	    ARCH="armv7"
	  fi
	fi

    # # OSx=$OSTYPE
    # if [ "$OS" = "android" ] && [ "$ARCH" = "arm" ]; then
    #     OS="linux"
    # fi

    # Set binary URL
   # BINARY_URL="https://github.com/rabilrbl/jiotv_go/releases/download/v3.6.0/jiotv_go-$OS-$ARCH"
    #BINARY_URL="https://github.com/rabilrbl/jiotv_go/releases/latest/download/jiotv_go-$OS-$ARCH"


	if [ "$OS" = "android" ] && [ "$ARCH" = "arm" ]; then
 		echo "WORK OF ART"
		BINARY_URL="https://objects.githubusercontent.com/github-production-release-asset-2e65be/682418985/0bbc55a1-a253-43ed-a733-1897f8c5ed1d?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=releaseassetproduction%2F20240905%2Fus-east-1%2Fs3%2Faws4_request&X-Amz-Date=20240905T171924Z&X-Amz-Expires=300&X-Amz-Signature=3544f1061f49eb5c8cd89d79d469a095a69c410c4ee8e304fdf7f924eb4e7bf6&X-Amz-SignedHeaders=host&actor_id=96260439&key_id=0&repo_id=682418985&response-content-disposition=attachment%3B%20filename%3Djiotv_go-android5-armv7&response-content-type=application%2Foctet-stream"
	else
		BINARY_URL="https://github.com/rabilrbl/jiotv_go/releases/download/v3.8.0/jiotv_go-$OS-$ARCH"
		#BINARY_URL="https://github.com/rabilrbl/jiotv_go/releases/latest/download/jiotv_go-$OS-$ARCH"
	fi

    # Download the binary
    curl -SL --progress-bar --retry 2 --retry-delay 2 -o "$HOME/.jiotv_go/bin/jiotv_go" "$BINARY_URL" || { echo "Failed to download binary"; exit 1; }

    echo "Step 4: Fetch the latest binary"

    # Make the binary executable
    chmod 755 "$HOME/.jiotv_go/bin/jiotv_go"
    termux-elf-cleaner "$HOME/.jiotv_go/bin/jiotv_go"
    
    
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
    $HOME/.jiotv_go/bin/jiotv_go epg gen
    am startservice -n com.termux/.app.TermuxService -a com.termux.service_execute
}

echo "Script : version v6.9.3x [5 series] test"

SDK_VERSION=$(getprop ro.build.version.sdk)

# Check if the SDK version is equal to or less than 23
if [ "$SDK_VERSION" -le 23 ]; then
	# Set OS to "android5"
	echo "VER = $SDK_VERSION"
fi



FILE_PATH="$HOME/.jiotv_go/bin/jiotv_go"

if [ ! -f "$FILE_PATH" ]; then
	 # Loop until the setup is complete
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
