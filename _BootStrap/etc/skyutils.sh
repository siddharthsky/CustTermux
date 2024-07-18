#!/bin/bash

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

login() {
	pkill -f "$HOME/.jiotv_go/bin/jiotv_go"
	starter=$($HOME/.jiotv_go/bin/jiotv_go bg run)
	
	PHONE_NUMBER=""
	send_otp() {
		source ~/.bashrc
		PHONE_NUMBER=$(termux-dialog text -t "Enter your jio number [10 digit] to login" | jq -r '.text')
		if [ $? != 0 ]; then
			echo "Canceled."
		fi

		url="http://localhost:5001/login/sendOTP"

		response=$(curl -s -X POST $url -H "Content-Type: application/json" -d "{\"number\": \"+91$PHONE_NUMBER\"}")
		echo "Please wait"
			wait_and_count 5
	}

	verify_otp() {

		otp=$(termux-dialog text -t "Enter your OTP" | jq -r '.text')
		if [ $? != 0 ]; then
			echo "Canceled."
		fi

		url="http://localhost:5001/login/verifyOTP"

		response=$(curl -s -X POST $url -H "Content-Type: application/json" -d "{\"number\": \"+91$PHONE_NUMBER\", \"otp\": \"$otp\"}")

		json_string=$(echo "$response" | jq -c .)

		if echo "$json_string" | grep -q "success"; then
			echo -e "\e[32mLogged in Successfully.\e[0m"
			prompt_login
		else
			echo -e "\e[31mLogin failed.\e[0m"
			prompt_unlogin
		fi

	}
	
	prompt_login() {
		termux-dialog spinner -v "LoggedIn" -t "Login Status"
	}
	
	prompt_unlogin() {
		termux-dialog spinner -v "Login Failed." -t "Login Status"
	}
	
	send_otp
	verify_otp
	sleep 1
	pkill -f "$HOME/.jiotv_go/bin/jiotv_go"

}

iptv() {
	select_iptv() {
		spr="SparkleTV2 - any app"	
		output=$(termux-dialog radio -t "Select an IPTV Player to autostart" -v "OTTNavigator,Televizo,SparkleTV,TiviMate,Kodi,$spr,none")
		
		
		selected=$(echo "$output" | jq -r '.text')
		if [ $? != 0 ]; then
			rm -rf "$HOME/.jiotv_go/bin/iptv.cfg"
			echo "NULL" > "$HOME/.jiotv_go/bin/iptv.cfg"
		fi
		

		if [ -n "$selected" ]; then
			echo "Selected: $selected"

			case "$selected" in
				OTTNavigator)
					rm -rf "$HOME/.jiotv_go/bin/iptv.cfg"
					echo "studio.scillarium.ottnavigator/studio.scillarium.ottnavigator.MainActivity" > "$HOME/.jiotv_go/bin/iptv.cfg"
					;;
				Televizo)
					rm -rf "$HOME/.jiotv_go/bin/iptv.cfg"
					echo "com.ottplay.ottplay/com.ottplay.ottplay.StartActivity" > "$HOME/.jiotv_go/bin/iptv.cfg"
					;;
				SparkleTV)
					rm -rf "$HOME/.jiotv_go/bin/iptv.cfg"
					echo "se.hedekonsult.sparkle/se.hedekonsult.sparkle.MainActivity" > "$HOME/.jiotv_go/bin/iptv.cfg"
					;;
				TiviMate)
					rm -rf "$HOME/.jiotv_go/bin/iptv.cfg"
					echo "ar.tvplayer.tv/ar.tvplayer.tv.ui.MainActivity" > "$HOME/.jiotv_go/bin/iptv.cfg"
					;;
				Kodi)
					rm -rf "$HOME/.jiotv_go/bin/iptv.cfg"
					echo "org.xbmc.kodi/org.xbmc.kodi.Splash" > "$HOME/.jiotv_go/bin/iptv.cfg"
					;;
				$spr)
					rm -rf "$HOME/.jiotv_go/bin/iptv.cfg"
					echo "com.skylake.siddharthsky.sparkletv2/com.skylake.siddharthsky.sparkletv2.MainActivity" > "$HOME/.jiotv_go/bin/iptv.cfg"
					;;
				none)
					rm -rf "$HOME/.jiotv_go/bin/iptv.cfg"
					echo "NULL" > "$HOME/.jiotv_go/bin/iptv.cfg"
					;;
			esac
		else
			rm -rf "$HOME/.jiotv_go/bin/iptv.cfg"
			echo "NULL" > "$HOME/.jiotv_go/bin/iptv.cfg"
		fi
	}
	
	prompt_gui() {
		termux-dialog spinner -v "Logged in Successfully." -t "Login Status"
	}
	
	select_iptv
	
	prompt_gui
		
	
}

update() {

	prompt_gui() {
		termux-dialog confirm -t "Update" -i "Do you want to update JioTV GO server?"
	}
	
	prompt_gui2() {
		termux-dialog spinner -v "Update:Done" -t "Update Status"
	}
	
	updater() {
		pkill -f "$HOME/.jiotv_go/bin/jiotv_go"
		rm "$HOME/.jiotv_go/bin/jiotv_go"
		sleep 1
		
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
			
			
		if [ "$OS" = "android" ] && [ "$ARCH" = "386" ]; then
			OS="linux"
		fi
		# Set binary URL
		BINARY_URL="https://github.com/rabilrbl/jiotv_go/releases/latest/download/jiotv_go-$OS-$ARCH"

        echo "Downloading Binary..."
        
		# Download the binary
		curl -SL --progress-bar --retry 2 --retry-delay 2 -o "$HOME/.jiotv_go/bin/jiotv_go" "$BINARY_URL" || { echo "Failed to download binary"; exit 1; }

        chmod 755 "$HOME/.jiotv_go/bin/jiotv_go"
	
	}
		
	if prompt_gui | grep -q "yes"; then
		#Update
		updater
		prompt_gui2
	else
		echo -e "\e[31mUser chose not to update.\e[0m"
		exit 0
	fi	
	
	


	
}



if [ "$1" == "login" ]; then
    login
elif [ "$1" == "iptv" ]; then
    iptv
elif [ "$1" == "update" ]; then
    update
else
    echo "Usage Error"
    exit 1
fi
