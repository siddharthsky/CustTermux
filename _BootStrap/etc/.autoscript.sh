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




Server_Runner() {
	$HOME/.jiotv_go/bin/jiotv_go -v
	echo "---------------------------"
	source ~/.bashrc #PATH update
	
	
	#------------------------------------------------
	#MODE CONFIG
	retrieve_first_line_mode() {
		local option=""
		# Check if mode.cfg exists and has content
		if [ -f "$HOME/.jiotv_go/bin/mode.cfg" ]; then
			option=$(head -n 1 "$HOME/.jiotv_go/bin/mode.cfg")
		else
			echo "mode.cfg file not found or empty."
		fi
		echo "$option"
	}

	retrieved_mode=$(retrieve_first_line_mode)

	
	#------------------------------------------------
	
	#------------------------------------------------
	#IPTV CONFIG
	retrieve_first_line_iptv() {
		local option=""
		# Check if iptv.cfg exists and has content
		if [ -f "$HOME/.jiotv_go/bin/iptv.cfg" ]; then
			option=$(head -n 1 "$HOME/.jiotv_go/bin/iptv.cfg")
		else
			echo "iptv.cfg file not found or empty."
		fi
		echo "$option"
	}
	
	retrieved_iptv=$(retrieve_first_line_iptv)
	
	if [ "$retrieved_iptv" = "NULL" ]; then
		echo ""
	else
		am start --user 0 -n $retrieved_iptv
	fi
	
	if [ "$retrieved_mode" = "MODE_ONE" ]; then
		echo "____MODE____DEFAULT____"
		termux-wake-lock
		echo "jiotv_go found, \$HOME/.jiotv_go/bin/jiotv_go run -P"
		$HOME/.jiotv_go/bin/jiotv_go run -P
	elif [ "$retrieved_mode" = "MODE_TWO" ]; then
		echo "____MODE____AUTOBOOT____"
		echo -e "Press \e[31mCTRL + C\e[0m to interrupt"
		$HOME/.jiotv_go/bin/jiotv_go run -P
	elif [ "$retrieved_mode" = "MODE_THREE" ]; then
		echo "____MODE____STANDALONE____"
		echo -e "Press \e[31mCTRL + C\e[0m to interrupt"
		$HOME/.jiotv_go/bin/jiotv_go run -P
		termux-open-url http://localhost:5001/
	else
		echo "____MODE____UNKNOWN____"
	fi

	
	#------------------------------------------------
}







gui_req() {
	pkg install termux-am jq termux-api -y
	rm -f $HOME/.termux/termux.properties
	touch $HOME/.termux/termux.properties
	chmod 755 $HOME/.termux/termux.properties
	echo "allow-external-apps = true" >> $HOME/.termux/termux.properties
	echo "If stuck here please clear app data"
}



check_termux_api() {
	check_package() {
		# Function to check if the package is available
		PACKAGE_NAME="com.termux.api"
		out="$(pm path $PACKAGE_NAME --user 0 2>&1 </dev/null)"
		
		# Check if the output contains the package path
		if [[ "$out" == *"$PACKAGE_NAME"* ]]; then
			echo -e "The package \e[32m$PACKAGE_NAME\e[0m is available."
			return 0
		else
			return 1
		fi

	}
	# Loop until the package is available
    while ! check_package; do
        echo "The package $PACKAGE_NAME is not installed. Checking again..."
		curl -L -o "$HOME/Tapi.apk" "https://github.com/termux/termux-api/releases/download/v0.50.1/termux-api_v0.50.1+github-debug.apk"
		chmod 755 "$HOME/Tapi.apk"
		termux-open "$HOME/Tapi.apk"
        sleep 10  # Wait for 5 seconds before checking again
    done

	echo -e "The package \e[32m$PACKAGE_NAME\e[0m is now available."
}






#------------------------------------------------
# Function to display menu and get selection for MODEs
select_mode() {
    # Create necessary directories
    if [[ ! -d "$HOME/.jiotv_go" ]]; then
        mkdir -p "$HOME/.jiotv_go"
    fi
    if [[ ! -d "$HOME/.jiotv_go/bin" ]]; then
        mkdir -p "$HOME/.jiotv_go/bin"
    fi
    
    MODE_ONE="DefaultMode: You open CustTermux to run the server, which then redirects to IPTV automatically."
    MODE_TWO="AutoBootMode: Server starts automatically at boot using [Termux:Boot]. You just need to open IPTV to watch TV. - Experimental"
	MODE_THREE="StandaloneMode: The server starts and redirects to the JioTV Go webpage."

    
    output=$(termux-dialog radio -t "Select Usage Method for CustTermux" -v "$MODE_ONE, $MODE_TWO,$MODE_THREE")

    selected=$(echo "$output" | jq -r '.text')
    if [ $? != 0 ]; then
        echo "Canceled."
        exit 1
    fi

    if [ -n "$selected" ]; then
        echo "Selected: $selected"

        case "$selected" in
            "$MODE_ONE")
                echo "MODE_ONE" > "$HOME/.jiotv_go/bin/mode.cfg"
                ;;
            "$MODE_TWO")
                echo "MODE_TWO" > "$HOME/.jiotv_go/bin/mode.cfg"
                ;;
			"$MODE_THREE")
                echo "MODE_THREE" > "$HOME/.jiotv_go/bin/mode.cfg"
                ;;
            *)
                echo "Unknown mode selected: $selected"
                exit 1
                ;;
        esac
    else
        echo "No mode selected, setting default mode (MODE_ONE)."
        echo "MODE_ONE" > "$HOME/.jiotv_go/bin/mode.cfg"
    fi
}






#------------------------------------------------
#Default Installation
Default_Installation() {
	# Check if jiotv_go exists
	
	
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
	if [[ ! -d "$HOME/.jiotv_go" ]]; then
		mkdir -p "$HOME/.jiotv_go"
	fi
	if [[ ! -d "$HOME/.jiotv_go/bin" ]]; then
		mkdir -p "$HOME/.jiotv_go/bin"
	fi
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
}
#------------------------------------------------






#-------------------------------
# Function to display menu and get selection
select_iptv() {
  output=$(termux-dialog radio -t "Select an IPTV Player to autostart" -v "OTTNavigator,Televizo,SparkleTV,TiviMate,Kodi,SparkleTV2,none")

  selected=$(echo "$output" | jq -r '.text')
  if [ $? != 0 ]; then
    echo "NULL" > "$HOME/.jiotv_go/bin/iptv.cfg"
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
		Kodi)
            echo "org.xbmc.kodi/org.xbmc.kodi.Splash" > "$HOME/.jiotv_go/bin/iptv.cfg"
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

#---------------------------------




#!/bin/bash
#############################################
# Global variable to store phone number
PHONE_NUMBER=""

# Function to send OTP
send_otp() {
  source ~/.bashrc
  $HOME/.jiotv_go/bin/jiotv_go bg run	
  # Fetch number from input using termux-dialog
  PHONE_NUMBER=$(termux-dialog text -t "Enter your Jio number to login" | jq -r '.text')
  if [ $? != 0 ]; then
    echo "Canceled."
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
  fi


  # Define the URL
  url="http://localhost:5001/login/verifyOTP"

  # Send OTP verification request
  response=$(curl -s -X POST $url -H "Content-Type: application/json" -d "{\"number\": \"+91$PHONE_NUMBER\", \"otp\": \"$otp\"}")
  $HOME/.jiotv_go/bin/jiotv_go bg kill
}

# Main execution




#------------------------------------------------
#MODE CONFIG


autoboot() {
    # Function to check if com.termux.boot package is available
	check_package() {
		# Function to check if the package is available

		PACKAGE_NAME="com.termux.boot"
		out="$(pm path $PACKAGE_NAME --user 0 2>&1 </dev/null)"
		
		# Check if the output contains the package path
		if [[ "$out" == *"$PACKAGE_NAME"* ]]; then
			echo -e "The package \e[32m$PACKAGE_NAME\e[0m is available."
			return 0
		else
			return 1
		fi
	}

	# Loop until the package is available
    while ! check_package; do
        echo "The package $PACKAGE_NAME is not installed. Checking again..."
		curl -L -o "$HOME/Tboot.apk" "https://github.com/termux/termux-boot/releases/download/v0.8.1/termux-boot-app_v0.8.1+github.debug.apk"
		chmod 755 "$HOME/Tboot.apk"
		termux-open "$HOME/Tboot.apk"
        sleep 10  # Wait for 10 seconds before checking again
    done

	boot_file() {
		mkdir -p "$HOME/.termux/boot/"
		rm -f "$HOME/.termux/boot/start_jio.sh"
		touch "$HOME/.termux/boot/start_jio.sh"

		echo "#!/data/data/com.termux/files/usr/bin/sh" > ~/.termux/boot/start_jio.sh
		echo "termux-wake-lock" >> ~/.termux/boot/start_jio.sh
		echo "termux-toast -g bottom 'Starting JioTV Go Server'" >> ~/.termux/boot/start_jio.sh
		echo "/data/data/com.termux/files/home/.jiotv_go/bin/jiotv_go run -public" >> ~/.termux/boot/start_jio.sh
		echo "$HOME/.jiotv_go/bin/jiotv_go bg run -P" >> ~/.termux/boot/start_jio.sh
		
		chmod 777 "$HOME/.termux/boot/start_jio.sh"
	}
	
	boot_file

	sleep 3
	am start --user 0 -n com.termux.boot/com.termux.boot.BootActivity
	sleep 3
	am start --user 0 -n com.termux/com.termux.app.TermuxActivity
	

	
}


FINAL_INSTALL() {
	retrieve_first_line() {
		local option=""
		# Check if mode.cfg exists and has content
		if [ -f "$HOME/.jiotv_go/bin/mode.cfg" ]; then
			option=$(head -n 1 "$HOME/.jiotv_go/bin/mode.cfg")
		else
			echo "mode.cfg file not found or empty."
		fi
		echo "$option"
	}

	retrieved_mode=$(retrieve_first_line)

	case "$retrieved_mode" in
		"MODE_ONE")
			echo "Setting DefaultMode"
			Default_Installation
			select_iptv
			send_otp
			verify_otp
			echo "jiotv_go has been downloaded and added to PATH. Running : \$HOME/.jiotv_go/bin/jiotv_go run -P"
			;;
		"MODE_TWO")
			echo "Setting AutoBoot"
			autoboot
			Default_Installation
			echo "NULL" > "$HOME/.jiotv_go/bin/iptv.cfg"
			send_otp
			verify_otp
			echo "jiotv_go has been downloaded and added to PATH. Running : \$HOME/.jiotv_go/bin/jiotv_go run -P"
			;;
		"MODE_THREE")
			echo "Setting Standalone mode"
			Default_Installation
			echo "NULL" > "$HOME/.jiotv_go/bin/iptv.cfg"
			send_otp
			verify_otp
			echo "jiotv_go has been downloaded and added to PATH."
			;;
		*)
			echo "mode.cfg file not found or empty."
			;;
	esac
}


######################################################################################
######################################################################################
######################################################################################
######################################################################################
######################################################################################
######################################################################################


# Check if jiotv_go exists
if [[ -f "$HOME/.jiotv_go/bin/jiotv_go" ]]; then
	Server_Runner
fi

sleep 2
echo "verision: 2"

FILE_PATH="$HOME/.jiotv_go/bin/run_check.cfg"

if [ ! -f "$FILE_PATH" ]; then
	mkdir -p "$HOME/.jiotv_go/bin/"
    echo "FIRST_RUN" > "$FILE_PATH"
	echo "-----------------------"
	echo "INSTALLATION -- PART 1"
	echo "-----------------------"
	gui_req
	echo "SECOND_RUN" > "$FILE_PATH"
	am startservice -n com.termux/.app.TermuxService -a com.termux.service_execute
else
    RUN_STATUS=$(cat "$FILE_PATH")

    if [ "$RUN_STATUS" == "FIRST_RUN" ]; then
       	echo "-----------------------"
        echo "INSTALLATION -- PART 1"
		echo "-----------------------"
		gui_req
		echo "SECOND_RUN" > "$FILE_PATH"
		am startservice -n com.termux/.app.TermuxService -a com.termux.service_execute
	elif [ "$RUN_STATUS" == "SECOND_RUN" ]; then
       	echo "-----------------------"
        echo "INSTALLATION -- PART 2"
		echo "-----------------------"
		check_termux_api
		select_mode
		FINAL_INSTALL
		echo "FINAL_RUN" > "$FILE_PATH"
		return 0
	elif [ "$RUN_STATUS" == "FINAL_RUN" ]; then
		return 0
    else 
       echo "Something Went Wrong : Clear App Data"
	   sleep 30
	   exit 1
    fi
fi



#------------------------------------------------



echo -e "\e[1;32mForce Stop CustTermux and Rerun.\e[0m"
echo -e "--or--"
echo -e "\e[1;33mRestart Device\e[0m"
echo -e "----------------------------"
echo -e "----------------------------"
echo -e "\e[0;36m-CustTermux by SiddharthSky\e[0m"
echo -e "----------------------------"

#Final Runner
if [[ -f "$HOME/.jiotv_go/bin/jiotv_go" ]]; then
	Server_Runner
fi

#############################################




