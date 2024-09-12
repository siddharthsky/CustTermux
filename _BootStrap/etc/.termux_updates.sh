#!/bin/bash

# Color codes
RED='\033[0;31m'      # Red
GREEN='\033[0;32m'    # Green
YELLOW='\033[0;33m'   # Yellow
NC='\033[0m'          # No Color

# Text components
prefix="##### "
suffix=" #####"
word="一期一会"
colors=($GREEN)

# Loop through each character of the word and apply color
echo -n "$prefix"
for (( i=0; i<${#word}; i++ )); do
  color=${colors[$((i % ${#colors[@]}))]}
  echo -ne "${color}${word:$i:1}${NC}"
done
echo "$suffix"

# Function to get the current version
get_version() {
    if [ ! -f "$HOME/.jiotv_go/bin/jiotv_go" ]; then
        echo "0.0.0"  # Default version
        return
    fi
    version_output=$($HOME/.jiotv_go/bin/jiotv_go -v)
    version=$(echo "$version_output" | grep -oP '(?<=version v)\d+\.\d+\.\d+')
    echo "$version"
}

# Compare two versions
version_less_than() {
    version1=$(echo "$1" | awk -F. '{ printf("%d%03d%03d\n", $1,$2,$3); }')
    version2=$(echo "$2" | awk -F. '{ printf("%d%03d%03d\n", $1,$2,$3); }')
    [[ $version1 -lt $version2 ]]
}

# Fetch the latest version from GitHub
get_latest_version() {
    curl -s https://api.github.com/repos/rabilrbl/jiotv_go/releases/latest | grep -oP '(?<=tag_name": "v)\d+\.\d+\.\d+'
}

# Function to update the binary
updater() {
    echo "Updating to the latest version..."

    # Identify the operating system
    OS=""
    case "$OSTYPE" in
        "linux-android"*) OS="android" ;;
        "linux-"*) OS="linux" ;;
        "darwin"*) OS="darwin" ;;
        *) echo "Unsupported operating system: $OSTYPE"; exit 1 ;;
    esac
    echo "Step 1: Identified operating system as $OS"

    # Identify the processor architecture
    ARCH=$(uname -m)
    case $ARCH in
        "x86_64") ARCH="amd64" ;;
        "aarch64" | "arm64") ARCH="arm64" ;;
        "i386" | "i686") ARCH="386" ;;
        "arm"*) ARCH="arm" ;;
        *) echo "Unsupported architecture: $ARCH"; exit 1 ;;
    esac
    echo "Step 2: Identified processor architecture as $ARCH"

    # Create necessary directories
    mkdir -p "$HOME/.jiotv_go/bin"
    echo "Step 3: Created \$HOME/.jiotv_go/bin"

    # Handle Android-specific cases
    if [ "$OS" = "android" ] && [ "$ARCH" = "arm" ]; then
        SDK_VERSION=$(getprop ro.build.version.sdk)
        if [ "$SDK_VERSION" -le 23 ]; then
            OS="android5"
            ARCH="armv7"
            pkg install termux-elf-cleaner -y
        fi
    fi

    # Set the binary URL and download it
    BINARY_URL="https://github.com/rabilrbl/jiotv_go/releases/latest/download/jiotv_go-$OS-$ARCH"
    curl -SL --progress-bar --retry 2 --retry-delay 2 -o "$HOME/.jiotv_go/bin/jiotv_go" "$BINARY_URL" || {
        echo "Failed to download binary"
        exit 1
    }
    echo "Step 4: Fetched the latest binary"

    # Make the binary executable
    chmod 755 "$HOME/.jiotv_go/bin/jiotv_go"
    if [ "$OS" = "android" ] && [ "$ARCH" = "arm" ]; then
        termux-elf-cleaner $HOME/.jiotv_go/bin/jiotv_go
    fi
    echo "Step 5: Granted executable permissions to the binary"

    # Add binary to PATH based on shell
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

# Check and update the version if needed
current_version=$(get_version)
target_version=$(get_latest_version)

if [ "$current_version" = "0.0.0" ]; then
    echo "Starting Installation"
else
    if version_less_than "$current_version" "$target_version"; then
        updater
    else
        echo "JioTV Go is up to date (version $current_version)."
    fi
fi
