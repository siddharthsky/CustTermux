# Command history tweaks:
# - Append history instead of overwriting
#   when shell exits.
# - When using history substitution, do not
#   exec command immediately.
# - Do not save to history commands starting
#   with space.
# - Do not save duplicated commands.
shopt -s histappend
shopt -s histverify
export HISTCONTROL=ignoreboth

# Default command line prompt.
PROMPT_DIRTRIM=2
PS1='\[\e[0;32m\]\w\[\e[0m\] \[\e[0;97m\]\$\[\e[0m\] '

# Handles nonexistent commands.
# If user has entered command which invokes non-available
# utility, command-not-found will give a package suggestions.
if [ -x /data/data/com.termux/files/usr/libexec/termux/command-not-found ]; then
	command_not_found_handle() {
		/data/data/com.termux/files/usr/libexec/termux/command-not-found "$1"
	}
fi

#######################################################
echo "---------------------------"
echo -e "\e[1;32mCustTERMUX - JioTV_GO\e[0m"



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




# Check if jiotv_go exists
if [[ -f "$HOME/.jiotv_go/bin/jiotv_go" ]]; then
	$HOME/.jiotv_go/bin/jiotv_go -v
	echo "---------------------------"
	source ~/.bashrc #PATH update
	
	#------------------------------------------------
	#IPTV CONFIG
	retrieve_first_line() {
		local option=""
		# Check if iptv.cfg exists and has content
		if [ -f "$HOME/.jiotv_go/bin/iptv.cfg" ]; then
			option=$(head -n 1 "$HOME/.jiotv_go/bin/iptv.cfg")
		else
			echo "iptv.cfg file not found or empty."
		fi
		echo "$option"
	}
	
	retrieved_option=$(retrieve_first_line)
	
	if [ "$retrieved_option" = "NULL" ]; then
        echo ""
    else
		am start --user 0 -n $retrieved_option
	fi
	
	#------------------------------------------------
    echo "jiotv_go found, \$HOME/.jiotv_go/bin/jiotv_go run -P"
    $HOME/.jiotv_go/bin/jiotv_go run -P
	sleep 20
    exit 0
fi


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


echo "Step 0: Updating Packages"
pkg install termux-am -y
pkg install jq -y
pkg install termux-api -y


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
if [[ ! -d "$HOME/.jiotv_go" ]]; then
    mkdir -p "$HOME/.jiotv_go"
fi
if [[ ! -d "$HOME/.jiotv_go/bin" ]]; then
    mkdir -p "$HOME/.jiotv_go/bin"
fi
echo "Step 3: Created \$HOME/.jiotv_go/bin"

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
        export PATH=$PATH:$HOME/.jiotv_go/bin
        echo "export PATH=$PATH:$HOME/.jiotv_go/bin" >> "$HOME/.zshrc"
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

#-------------------------------
# Function to display menu and get selection
select_option() {
  output=$(termux-dialog radio -t "Select an IPTV Player to autostart" -v "OTTNavigator,Televizo,SparkleTV,TiviMate,SparkleTV2,none")
  echo "termux-dialog output: $output"  # Debugging line

  selected=$(echo "$output" | jq -r '.text')
  if [ $? != 0 ]; then
    echo "Canceled."
    exit 1
  fi
  

  if [ -n "$selected" ]; then
    echo "Selected: $selected"

    case "$selected" in
        OTTNavigator)
            echo "studio.scillarium.ottnavigator/studio.scillarium.ottnavigator.MainActivity" > "$HOME/.jiotv_go/bin/iptv.cfg"
            ;;
        Televizo)
            echo "com.ottplay.ottplay/com.ottplay.ottplay.StartActivity" > "$HOME/.jiotv_go/bin/iptv.cfg"
            ;;
        SparkleTV)
            echo "se.hedekonsult.sparkle/se.hedekonsult.sparkle.MainActivity" > "$HOME/.jiotv_go/bin/iptv.cfg"
            ;;
        TiviMate)
            echo "ar.tvplayer.tv/ar.tvplayer.tv.ui.MainActivity" > "$HOME/.jiotv_go/bin/iptv.cfg"
            ;;
        SparkleTV2)
            echo "com.skylake.siddharthsky.sparkletv2/com.skylake.siddharthsky.sparkletv2.MainActivity" > "$HOME/.jiotv_go/bin/iptv.cfg"
            ;;
        none)
            echo "NULL" > "$HOME/.jiotv_go/bin/iptv.cfg"
            ;;
    esac
  else
    echo "NULL" > "$HOME/.jiotv_go/bin/iptv.cfg"
  fi
}

# Main execution
select_option
#---------------------------------


source ~/.bashrc


#echo -e "-----------------------------------"
#echo "Redirecting to login page."
#echo -e "\e[1;32mURL:https://localhost:5001\e[0m"
#echo -e "-----------------------------------"

#sleep 7

echo "jiotv_go has been downloaded and added to PATH. Running : \$HOME/.jiotv_go/bin/jiotv_go run -P"

#termux-open-url http:localhost:5001

$HOME/.jiotv_go/bin/jiotv_go bg run -a -P

#!/bin/bash
#############################################
# Global variable to store phone number
PHONE_NUMBER=""

# Function to send OTP
send_otp() {
  # Fetch number from input using termux-dialog
  PHONE_NUMBER=$(termux-dialog text -t "Enter your Jio number to login" | jq -r '.text')
  if [ $? != 0 ]; then
    echo "Canceled."
    exit 1
  fi

  if [ -z "$PHONE_NUMBER" ]; then
    termux-dialog confirm -t "Error" -i "Number is required!"
    exit 1
  fi

  # Define the URL
  url="http://localhost:5001/login/sendOTP"

  # Send OTP request
  response=$(curl -s -X POST $url -H "Content-Type: application/json" -d "{\"number\": \"+91$PHONE_NUMBER\"}")
}

# Function to verify OTP
verify_otp() {
  # Fetch OTP from input using termux-dialog
  otp=$(termux-dialog text -t "Enter your OTP" | jq -r '.text')
  if [ $? != 0 ]; then
    echo "Canceled."
    exit 1
  fi

  if [ -z "$otp" ]; then
    termux-dialog confirm -t "Error" -i "OTP is required!"
    exit 1
  fi

  # Define the URL
  url="http://localhost:5001/login/verifyOTP"

  # Send OTP verification request
  response=$(curl -s -X POST $url -H "Content-Type: application/json" -d "{\"number\": \"+91$PHONE_NUMBER\", \"otp\": \"$otp\"}")
}

# Main execution
send_otp
verify_otp

$HOME/.jiotv_go/bin/jiotv_go bg kill

echo -e "\e[1;32mForce Stop CustTermux and Rerun.\e[0m"
echo -e "--or--"
echo -e "\e[1;33mRestart Device\e[0m"
echo -e "----------------------------"
echo -e "----------------------------"
echo -e "\e[0;36m-CustTermux by SiddharthSky\e[0m"
echo -e "----------------------------"

$HOME/.jiotv_go/bin/jiotv_go run -P
#############################################



