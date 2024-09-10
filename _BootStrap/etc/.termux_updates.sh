#!/bin/bash

RED='\033[0;31m'      # Red
GREEN='\033[0;32m'    # Green
YELLOW='\033[0;33m'   # Yellow
NC='\033[0m'          # No Color

# The fixed part of the text
prefix="##### "
suffix=" #####"

# The word to change color
word="一期一会"

# Array of colors
colors=($GREEN)

# Loop through each character of the word
echo -n "$prefix"
for (( i=0; i<${#word}; i++ )); do
  # Use the modulus operator to cycle through colors
  color=${colors[$((i % ${#colors[@]}))]}
  
  # Print the character with the selected color
  echo -ne "${color}${word:$i:1}${NC}"
done
echo "$suffix"


get_version() {
    version_output=$($HOME/.jiotv_go/bin/jiotv_go -v)
    version=$(echo "$version_output" | grep -oP '(?<=version v)\d+\.\d+\.\d+')
    echo "$version"
}

version_less_than() {
    version1=$(echo "$1" | awk -F. '{ printf("%d%03d%03d\n", $1,$2,$3); }')
    version2=$(echo "$2" | awk -F. '{ printf("%d%03d%03d\n", $1,$2,$3); }')
    [[ $version1 -lt $version2 ]]
}

get_latest_version() {
    curl -s https://api.github.com/repos/rabilrbl/jiotv_go/releases/latest | grep -oP '(?<=tag_name": "v)\d+\.\d+\.\d+'
}


updater() {
    echo "Updating to the latest version..."

    # Detect the operating system
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

    # Detect the processor architecture
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

    # Handle specific Android cases
    if [ "$OS" = "android" ] && [ "$ARCH" = "arm" ]; then
        SDK_VERSION=$(getprop ro.build.version.sdk)
        if [ "$SDK_VERSION" -le 23 ]; then
            OS="android5"
            ARCH="armv7"
            pkg install termux-elf-cleaner -y
        fi
    fi

    # Set binary URL
    BINARY_URL="https://github.com/rabilrbl/jiotv_go/releases/latest/download/jiotv_go-$OS-$ARCH"

    # Download the binary
    curl -SL --progress-bar --retry 2 --retry-delay 2 -o "$HOME/.jiotv_go/bin/jiotv_go" "$BINARY_URL" || { 
        echo "Failed to download binary"
        exit 1
    }

    echo "Step 4: Fetched the latest binary"

    # Make the binary executable
    chmod 755 "$HOME/.jiotv_go/bin/jiotv_go"

    if [ "$OS" = "android" ] && [ "$ARCH" = "arm" ]; then
        SDK_VERSION=$(getprop ro.build.version.sdk)
        if [ "$SDK_VERSION" -le 23 ]; then
            termux-elf-cleaner "$HOME/.jiotv_go/bin/jiotv_go"
        fi
    fi

    echo "Step 5: Granted executable permissions to the binary"

    # Add binary to PATH based on shell type
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
            echo "Please restart your terminal or run 'source $HOME/.config/fish/config.fish'"
            ;;
        *)
            echo "Unsupported shell: $SHELL_NAME"
            exit 1
            ;;
    esac
}


current_version=$(get_version)
target_version=$(get_latest_version)

if version_less_than "$current_version" "$target_version"; then
    updater
else
    echo "JioTV Go is up to date (version $current_version)."
fi




# echo "##################### ひらめき ############################"
#echo -e "${RED}WARNING:${NC} Please update the app to the latest version whenever it becomes available."
#echo -e "${RED}NOTE:${NC} To be on the safer side, we are renaming the repositories. Stay tuned for the next updates."
#echo -e "${RED}IMPORTANT:${NC} After ${GREEN}01.09.2024${NC}, you will not be able to install older versions of CustTermux."

#echo ""

# echo -e "${RED}NOTICE:${NC} ${NC}To avoid potential copyright issues with Jio,${NC}"
# echo -e "${YELLOW}we will be renaming our GitHub repository.${NC}"
# echo -e "${GREEN}Update will be provided soon.${NC}"
