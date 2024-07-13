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



wait_and_count() {
    local duration=$1
    local counter=0
    local spinner="/-\|"
    local bar_length=30

    echo "[$duration] Processing..."
    while [ $counter -lt $duration ]; do
        local progress=$((counter * bar_length / duration))
        printf "\r[\033[0;32m%-*s\033[0m] %d%% %c" $bar_length $(printf '#%.0s' $(seq 1 $progress)) $((counter * 100 / duration)) ${spinner:counter%4:1}
        sleep 1
        ((counter++))
    done
    printf "\r[\033[0;32m%-*s\033[0m] 100%% \n" $bar_length $(printf '#%.0s' $(seq 1 $bar_length))
}


IP_ADD=""
get_ip_address() {
    local ip_address=$(termux-wifi-connectioninfo | grep -oP '(?<="ip": ")[^"]+')
    IP_ADD="$ip_address"
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
		termux-wake-lock
		sleep 1
		am start --user 0 -n $retrieved_iptv
		
	fi
	
	if [ "$retrieved_mode" = "MODE_ONE" ]; then
		echo "____MODE____DEFAULT____"
		#termux-wake-lock
		echo "jiotv_go found, \$HOME/.jiotv_go/bin/jiotv_go run -P"
		$HOME/.jiotv_go/bin/jiotv_go run -P
	elif [ "$retrieved_mode" = "MODE_TWO" ]; then
		echo "____MODE____SERVERMODE____"
		termux-wake-lock
		echo -e "Press \e[31mCTRL + C\e[0m to interrupt"
		$HOME/.jiotv_go/bin/jiotv_go run -P
	elif [ "$retrieved_mode" = "MODE_THREE" ]; then
		echo "____MODE____STANDALONE____"
		termux-wake-lock
		echo -e "Press \e[31mCTRL + C\e[0m to interrupt"
		am start -a android.intent.action.VIEW -d "http://localhost:5001/" -e "android.support.customtabs.extra.SESSION" null
		#termux-open-url http://localhost:5001/
		$HOME/.jiotv_go/bin/jiotv_go run -P
	else
		echo "____MODE____UNKNOWN____"
	fi

	
	#------------------------------------------------
}




select_autoboot_or_not() {
    MODE_ONE="NO"
    MODE_TWO="YES - This will install TERMUX:BOOT [Experimental]"

	touch $HOME/.jiotv_go/bin/autoboot_or_not.cfg 
    
    output=$(termux-dialog radio -t "Do you want to autostart Server at boot?" -v "$MODE_ONE, $MODE_TWO")

    selected=$(echo "$output" | jq -r '.text')
    if [ $? != 0 ]; then
        echo "Canceled."
        exit 1
    fi

    if [ -n "$selected" ]; then
        echo "Selected: $selected"

        case "$selected" in
            "$MODE_ONE")
                echo "NO" > "$HOME/.jiotv_go/bin/autoboot_or_not.cfg"
                ;;
            "$MODE_TWO")
                echo "YES" > "$HOME/.jiotv_go/bin/autoboot_or_not.cfg"
                ;;
            *)
                echo "Unknown mode selected: $selected"
                exit 1
                ;;
        esac
    else
        echo "No mode selected, setting default mode (MODE_ONE)."
        echo "NO" > "$HOME/.jiotv_go/bin/autoboot_or_not.cfg"
    fi
}




gui_req() {
	pkg install termux-am jq termux-api -y
	rm -f $HOME/.termux/termux.properties
	touch $HOME/.termux/termux.properties
	chmod 755 $HOME/.termux/termux.properties
	echo "allow-external-apps = true" >> $HOME/.termux/termux.properties
	#am start --user 0 -a android.settings.action.MANAGE_OVERLAY_PERMISSION -d "package:com.termux"
	echo "If stuck, Please clear app data and restart your device."

}



check_termux_api() {

	app_permission_check (){
		touch "$HOME/.jiotv_go/bin/permission.cfg"
		chmod 755 "$HOME/.jiotv_go/bin/permission.cfg"
		quick_var=$(head -n 1 "$HOME/.jiotv_go/bin/permission.cfg")	
		if [ "$quick_var" = "OVERLAY=TRUE" ]; then
			""
		else
			retrieved_boot_or_not=$(head -n 1 "$HOME/.jiotv_go/bin/permission.cfg")
			termux-toast -g bottom 'Give premisson and press back to continue'
			am start --user 0 -a android.settings.MANAGE_UNKNOWN_APP_SOURCES -d "package:com.termux"
			echo "waiting for app install permissions"
			wait_and_count 20
			echo "OVERLAY=TRUE" > "$HOME/.jiotv_go/bin/permission.cfg"
		fi

	}
	

	check_package() {
		app_permission_check
		# Function to check if the package is available
		PACKAGE_NAME="com.termux.api"
		out="$(pm path $PACKAGE_NAME --user 0 2>&1 </dev/null)"
		
		# Check if the output contains the package path
		if [[ "$out" == *"$PACKAGE_NAME"* ]]; then
			echo -e "The package \e[32m$PACKAGE_NAME\e[0m is available."
			am start --user 0 -n com.termux/com.termux.app.TermuxActivity
			echo "If stuck, Please clear app data and restart your device."
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
        wait_and_count 20
    done

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
    
	MODE_ONE="Default Mode: Launch CustTermux to run server & auto-redirect to IPTV player [for TV]."
	MODE_TWO="Server Mode: Run server on your phone and watch on your TV [for Phone]."
	MODE_THREE="Standalone App Mode: Access JioTV Go via webpage [for Phone]."

    
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
	spr="SparkleTV2 - any app"	
	output=$(termux-dialog radio -t "Select an IPTV Player to autostart" -v "OTTNavigator,Televizo,SparkleTV,TiviMate,Kodi,$spr,none")

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
			$spr)
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
	# Fetch number from input using termux-dialog
	PHONE_NUMBER=$(termux-dialog text -t "Enter your jio number [10 digit] to login" | jq -r '.text')
	if [ $? != 0 ]; then
		echo "Canceled."
	fi



	url="http://localhost:5001/login/sendOTP"

	# Send OTP request
	response=$(curl -s -X POST $url -H "Content-Type: application/json" -d "{\"number\": \"+91$PHONE_NUMBER\"}")
	sleep 1
}

# Function to verify OTP
verify_otp() {
	# Fetch OTP from input using termux-dialog
	otp=$(termux-dialog text -t "Enter your OTP" | jq -r '.text')
	if [ $? != 0 ]; then
		echo "Canceled."
	fi


	url="http://localhost:5001/login/verifyOTP"

	# Send OTP verification request
	response=$(curl -s -X POST $url -H "Content-Type: application/json" -d "{\"number\": \"+91$PHONE_NUMBER\", \"otp\": \"$otp\"}")

	json_string=$(echo "$response" | jq -c .)

	if echo "$json_string" | grep -q "success"; then
		echo -e "\e[32mLogged in Successfully.\e[0m"
	else
		echo -e "\e[31mLogin failed.\e[0m"
	fi

}

# Main execution




#------------------------------------------------
#MODE CONFIG


autoboot() {
	app_permission_check (){
		touch "$HOME/.jiotv_go/bin/permission.cfg"
		chmod 755 "$HOME/.jiotv_go/bin/permission.cfg"
		quick_var=$(head -n 1 "$HOME/.jiotv_go/bin/permission.cfg")	
		if [ "$quick_var" = "OVERLAY=TRUE" ]; then
			""
		else
			retrieved_boot_or_not=$(head -n 1 "$HOME/.jiotv_go/bin/permission.cfg")
			am start --user 0 -a android.settings.MANAGE_UNKNOWN_APP_SOURCES -d "package:com.termux"
			echo "Waiting for app install permissions"
			wait_and_count 15

		fi

	}

    # Function to check if com.termux.boot package is available
	check_package() {
		app_permission_check
		# Function to check if the package is available

		PACKAGE_NAME="com.termux.boot"
		out="$(pm path $PACKAGE_NAME --user 0 2>&1 </dev/null)"
		
		# Check if the output contains the package path
		if [[ "$out" == *"$PACKAGE_NAME"* ]]; then
			echo -e "The package \e[32m$PACKAGE_NAME\e[0m is available."
			am start --user 0 -n com.termux/com.termux.app.TermuxActivity
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
        wait_and_count 15
    done

	boot_file() {
		mkdir -p "$HOME/.termux/boot/"
		rm -f "$HOME/.termux/boot/start_jio.sh"
		touch "$HOME/.termux/boot/start_jio.sh"

		echo "Creating Boot files: Please wait..."

		echo "#!/data/data/com.termux/files/usr/bin/sh" > ~/.termux/boot/start_jio.sh
		echo "termux-wake-lock" >> ~/.termux/boot/start_jio.sh
		echo "termux-toast -g bottom 'Starting JioTV Go Server'" >> ~/.termux/boot/start_jio.sh
		echo "/data/data/com.termux/files/home/.jiotv_go/bin/jiotv_go run -public" >> ~/.termux/boot/start_jio.sh
		echo "$HOME/.jiotv_go/bin/jiotv_go bg run -P" >> ~/.termux/boot/start_jio.sh
		
		chmod 777 "$HOME/.termux/boot/start_jio.sh"
		wait_and_count 10
	}
	
	boot_file

	Install_Alert=$(termux-dialog spinner -v "Termux:Boot Installed Successfully" -t "CustTermux")
	

	
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
			echo "Setting Default Mode"

			select_autoboot_or_not
			
			retrieved_boot_or_not=$(head -n 1 "$HOME/.jiotv_go/bin/autoboot_or_not.cfg")

			case "$retrieved_boot_or_not" in
                "NO")
			;;
			    "YES")
				echo "Setting AutoBoot"
				autoboot
            ;;
                *)
			esac

			select_iptv
			$HOME/.jiotv_go/bin/jiotv_go bg run	
			send_otp
			verify_otp
			$HOME/.jiotv_go/bin/jiotv_go bg kill
			echo "Running : \$HOME/.jiotv_go/bin/jiotv_go run -P"
			;;
		"MODE_TWO")
			echo "Setting Server Mode"
			#autoboot
			echo "NULL" > "$HOME/.jiotv_go/bin/iptv.cfg"
			$HOME/.jiotv_go/bin/jiotv_go bg run	
			send_otp
			verify_otp
			$HOME/.jiotv_go/bin/jiotv_go bg kill
			echo "Running : \$HOME/.jiotv_go/bin/jiotv_go run -P"
			;;
		"MODE_THREE")
			echo "Setting Standalone Mode"
			echo "NULL" > "$HOME/.jiotv_go/bin/iptv.cfg"
			$HOME/.jiotv_go/bin/jiotv_go bg run	
			send_otp
			verify_otp
			$HOME/.jiotv_go/bin/jiotv_go bg kill
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
echo "Script :version 5"

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
		Default_Installation
		FINAL_INSTALL
		echo "FINAL_RUN" > "$FILE_PATH"
		Server_Runner
		echo -e "----------------------------"
		echo -e "\e[0;36m-CustTermux by SiddharthSky\e[0m"
		echo -e "----------------------------"
	elif [ "$RUN_STATUS" == "FINAL_RUN" ]; then
		echo ""
    else 
       echo "Something Went Wrong : Clear App Data"
	   sleep 30
	   exit 1
    fi
fi



#------------------------------------------------




