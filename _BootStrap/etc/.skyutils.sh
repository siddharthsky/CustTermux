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

theshowrunner() {
	bash $PREFIX/etc/bash.bashrc
}

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

TheShowRunner1() {
	a_username=$(whoami)

	am start -a com.termux.SaveReceiver -n com.termux/.SkySharedPrefActivity --es key server_setup_username --es value $a_username

	get_value_from_key "server_setup_isLocal" "VARIABLE03"
 
	if [ "$VARIABLE03" == "Yes" ]; then
 		echo -e "\e[32mRunning Server Locally\e[0m"
		$HOME/.jiotv_go/bin/jiotv_go run
  		#$HOME/.jiotv_go/bin/jiotv_go bg run
	else
 		$HOME/.jiotv_go/bin/jiotv_go run -P
		#$HOME/.jiotv_go/bin/jiotv_go bg run -a -P
	fi
 	am start --user 0 -a com.termux.SKY_ACTION -n com.termux/.SkyActionActivity -e mode "loginstatus2"
}

TheShowRunner2() {
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
	#$HOME/.jiotv_go/bin/jiotv_go bg run
 	am start --user 0 -a com.termux.SKY_ACTION -n com.termux/.SkyActionActivity -e mode "loginstatus2"  &
        $HOME/.jiotv_go/bin/jiotv_go run --port $port_to_use
    else
        echo -e "\e[32mRunning Server on port $port_to_use\e[0m"
        termux-wake-lock
	#$HOME/.jiotv_go/bin/jiotv_go bg run --args "--port $port_to_use --public"
 	am start --user 0 -a com.termux.SKY_ACTION -n com.termux/.SkyActionActivity -e mode "loginstatus2"  &
	$HOME/.jiotv_go/bin/jiotv_go run --port $port_to_use --public
        
    fi

     
}

TheShowRunner2_nologin() {
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
	#$HOME/.jiotv_go/bin/jiotv_go bg run
 	#am start --user 0 -a com.termux.SKY_ACTION -n com.termux/.SkyActionActivity -e mode "loginstatus2"  &
        $HOME/.jiotv_go/bin/jiotv_go run --port $port_to_use
    else
        echo -e "\e[32mRunning Server on port $port_to_use\e[0m"
        termux-wake-lock
	#$HOME/.jiotv_go/bin/jiotv_go bg run --args "--port $port_to_use --public"
 	#am start --user 0 -a com.termux.SKY_ACTION -n com.termux/.SkyActionActivity -e mode "loginstatus2"  &
	$HOME/.jiotv_go/bin/jiotv_go run --port $port_to_use --public
        
    fi

     
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
		termux-dialog spinner -v "IPTV Changed" -t "IPTV Status"
	}
	
	select_iptv
	
	prompt_gui
		
	
}

iptvrunner() {
	retrieved_iptv=$(retrieve_first_line "$HOME/.jiotv_go/bin/iptv.cfg")

	#am start --user 0 -n com.termux/com.termux.app.TermuxActivity

	#sleep 0.5

	if [ "$retrieved_iptv" != "NULL" ]; then
		am start --user 0 -n "$retrieved_iptv"
		exit 0
	fi
	exit 0

}

reinstall() {
	echo "-----------------------"
	echo "Reinstall Utility"
	echo "-----------------------"
	pkill -f "$HOME/.jiotv_go/bin/jiotv_go"
 	wait_and_count 3

	reinstaller() {
		echo "Removing Server Files..."
		rm -rf "$HOME/.jiotv_go/bin/"
		rm  "$HOME/.autoscript.sh"
		rm  "$HOME/.autoscript_x.sh"
		rm  "$HOME/.autoscript_xm.sh"
		rm  "$HOME/.skyutils.sh"
  		rm  "$HOME/.autoscript_xz.sh"


  		am startservice -n com.termux/.app.TermuxService -a com.termux.service_execute
    		#am start --user 0 -a com.termux.SKY_ACTION -n com.termux/.SkyActionActivity -e mode "setup_finisher"


	}

  	reinstaller
	
}

reinstall2() {
	echo "-----------------------"
	echo "Reinstall Utility"
	echo "-----------------------"
	pkill -f "$HOME/.jiotv_go/bin/jiotv_go"
 	wait_and_count 3

	reinstaller() {
		echo "Removing Server Files..."
  		rm -rf "$HOME/*"
  		#am startservice -n com.termux/.app.TermuxService -a com.termux.service_execute
    		#am start --user 0 -a com.termux.SKY_ACTION -n com.termux/.SkyActionActivity -e mode "setup_finisher"
	}

  	reinstaller
	
}

update() {
	echo "-----------------------"
	echo "Update Utility"
	echo "-----------------------"
 	pkill -f "$HOME/.jiotv_go/bin/jiotv_go"
 	wait_and_count 3

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

		echo "Updating binaries..."

		# Download the binary
		curl -SL --progress-bar --retry 2 --retry-delay 2 -o "$HOME/.jiotv_go/bin/jiotv_go" "$BINARY_URL" || { echo "Failed to download binary"; exit 1; }

  		chmod 755 "$HOME/.jiotv_go/bin/jiotv_go"
	
	}
		
	updater
	am startservice -n com.termux/.app.TermuxService -a com.termux.service_execute
 	#am start --user 0 -a com.termux.SKY_ACTION -n com.termux/.SkyActionActivity -e mode "setup_finisher"
}

runcode() {

	#code = $1
	code=$(termux-dialog text -t "Enter command" | jq -r '.text')
	if [ $? != 0 ]; then
		echo "Canceled."
	fi

 	echo "Running..."
 	
 	$code

   	sleep 2
}


sendotp() {
	PHONE_NUMBER=$1
 	pkill -f "$HOME/.jiotv_go/bin/jiotv_go"
 	starter=$($HOME/.jiotv_go/bin/jiotv_go bg run)
	sleep 2
	if [ -z "$PHONE_NUMBER" ]; then
		echo "Phone number is required."
		exit 1
	fi
	
	url="http://localhost:5001/login/sendOTP"
	
	response=$(curl -s -X POST $url -H "Content-Type: application/json" -d "{\"number\": \"+91$PHONE_NUMBER\"}")
	echo "$response"
 	#echo "Please wait"
        #wait_and_count 5
	pkill -f "$HOME/.jiotv_go/bin/jiotv_go"
}



verifyotp() {
	PHONE_NUMBER=$1
 	otp=$2
 	pkill -f "$HOME/.jiotv_go/bin/jiotv_go"
  	starter=$($HOME/.jiotv_go/bin/jiotv_go bg run)
  	echo "Processing OTP..."
  	wait_and_count 5

    	if [ -z "$otp" ]; then
		echo "OTP is required."
		exit 1
    	fi
	
	url="http://localhost:5001/login/verifyOTP"
	
	response=$(curl -s -X POST $url -H "Content-Type: application/json" -d "{\"number\": \"+91$PHONE_NUMBER\", \"otp\": \"$otp\"}")

 	echo "Verifying OTP..."
	
	json_string=$(echo "$response" | jq -c .)
	sleep 2

	if echo "$json_string" | grep -q "success"; then
		echo -e "\e[32mLogged in Successfully.\e[0m"
		sleep 3
	else
		echo -e "\e[31mLogin failed.\e[0m"
		sleep 3
	fi
	
	sleep 1
	exit 0
 	pkill -f "$HOME/.jiotv_go/bin/jiotv_go"
}

sendotpx() {
	PHONE_NUMBER=$1
	sleep 2
	if [ -z "$PHONE_NUMBER" ]; then
		echo "Phone number is required."
		exit 1
	fi
	
	url="http://localhost:5001/login/sendOTP"
	
	response=$(curl -s -X POST $url -H "Content-Type: application/json" -d "{\"number\": \"+91$PHONE_NUMBER\"}")
	echo "$response"
 	#echo "Please wait"
        #wait_and_count 5
}



verifyotpx() {
	PHONE_NUMBER=$1
 	otp=$2
  	echo "Processing OTP..."
  	wait_and_count 5

    	if [ -z "$otp" ]; then
		echo "OTP is required."
		exit 1
    	fi
	
	url="http://localhost:5001/login/verifyOTP"
	
	response=$(curl -s -X POST $url -H "Content-Type: application/json" -d "{\"number\": \"+91$PHONE_NUMBER\", \"otp\": \"$otp\"}")

 	echo "Verifying OTP..."
	
	json_string=$(echo "$response" | jq -c .)
	sleep 2

	if echo "$json_string" | grep -q "success"; then
		echo -e "\e[32mLogged in Successfully.\e[0m"
		sleep 3
	else
		echo -e "\e[31mLogin failed.\e[0m"
		sleep 3
	fi
	
	sleep 1
	exit 0
}

exitpath_alt() {
	echo "-----------------------"
	echo -e "\033[31mStopping Server CustTermux\033[0m"
	echo "-----------------------"
	pkill -f "$HOME/.jiotv_go/bin/jiotv_go"
	pkill -f "jiotv_go"
 	echo "-----------------------"
	echo -e "\033[31mExiting CustTermux\033[0m"
	echo "-----------------------"
}

exitpath() {
	echo "-----------------------"
	echo -e "\033[31mStopping Server CustTermux\033[0m"
	echo "-----------------------"
	pkill -f "$HOME/.jiotv_go/bin/jiotv_go"
	pkill -f "jiotv_go"
 	echo "-----------------------"
	echo -e "\033[31mExiting CustTermux\033[0m"
	echo "-----------------------"
 	am start -a com.termux.SaveReceiver -n com.termux/.SkySharedPrefActivity --es key isExit --es value yesExit
}

epg_on() {
	echo "-----------------------"
	echo "EPG Utility"
	echo "-----------------------"
 	echo "Generating EPG for the first time."
 	$HOME/.jiotv_go/bin/jiotv_go epg gen
  	am start -a com.termux.SaveReceiver -n com.termux/.SkySharedPrefActivity --es key server_setup_isEPG --es value Yes
	sleep 1
 	exit 0
}

epg_off() {
	echo "-----------------------"
	echo "EPG Utility"
	echo "-----------------------"
 	$HOME/.jiotv_go/bin/jiotv_go epg del
  	am start -a com.termux.SaveReceiver -n com.termux/.SkySharedPrefActivity --es key server_setup_isEPG --es value No
	sleep 1
 	exit 0
 }

 ssh_on() {
	echo "-----------------------"
	echo "SSH Utility"
	echo "-----------------------"
 	echo "Checking Required Packages"
  
  	pkg install openssh -y
        pkg install expect -y
	# pkg install make -y

	ssh_passwd_intent
	
	pkill sshd
 
	sshd
  	am start -a com.termux.SaveReceiver -n com.termux/.SkySharedPrefActivity --es key server_setup_isSSH --es value Yes
   
   	echo "Started SSH"
    	wait_and_count 3
     	exit 0
}

ssh_off() {
	echo "-----------------------"
	echo "SSH Utility"
	echo "-----------------------"
  	pkill sshd
 	wait_and_count 1
	am start -a com.termux.SaveReceiver -n com.termux/.SkySharedPrefActivity --es key server_setup_isSSH --es value No
	echo "Stopped SSH"
}

ssh_passwd() {
	set password "letmein"

	spawn passwd
 
	expect "New password:"
	send "$password\r"
	expect "Retype new password:"
	send "$password\r"
 
	expect eof
}

ssh_passwd_intent() {
	am startservice \
    -n com.termux/com.termux.app.RunCommandService \
    -a com.termux.RUN_COMMAND \
    --es "com.termux.RUN_COMMAND_PATH" "/data/data/com.termux/files/home/.set_password.exp" \
    --ez "com.termux.RUN_COMMAND_BACKGROUND" true \
    --ei "com.termux.RUN_COMMAND_SESSION_ACTION" 0 \
    --es "com.termux.RUN_COMMAND_WORKDIR" "/data/data/com.termux/files/home"
}

write_port() {
    port_num=$1
    file="$HOME/.jiotv_go/bin/server_port.cfg"
    touch "$file"
    chmod 755 "$file"
    echo "$port_num" > "$file"
}

custominstall() {
	ARCH=$1
	OS=$2
 	echo "-----------------------"
	echo "Custom Install Utility"
	echo "-----------------------"
 
 	rm -rf "$HOME/.jiotv_go/bin/jiotv_go"
  
 	BINARY_URL="https://github.com/rabilrbl/jiotv_go/releases/latest/download/jiotv_go-$OS-$ARCH" 
     	mkdir -p "$HOME/.jiotv_go/bin"
      	echo "[#] Downloading Latest JioTV GO -os $OS -arch $ARCH"
   	curl -SL --progress-bar --retry 2 --retry-delay 2 -o "$HOME/.jiotv_go/bin/jiotv_go" "$BINARY_URL" || { echo "Failed to download binary"; exit 1; }
   	chmod 755 "$HOME/.jiotv_go/bin/jiotv_go"
    	echo "Installed JioTV GO - $OS - $ARCH"
     	echo "Restart CustTermux"
      	exit 1
}

termuxinfo() {
	termux-info
 	exit 1

}





if [ "$1" == "login" ]; then
 	   login
elif [ "$1" == "sendotp" ]; then
 	   sendotp "$2"
elif [ "$1" == "verifyotp" ]; then
  	  verifyotp "$2" "$3"
elif [ "$1" == "sendotpx" ]; then
  	  sendotp "$2"
elif [ "$1" == "verifyotpx" ]; then
  	  verifyotp "$2" "$3"
elif [ "$1" == "theshowrunner" ]; then
  	  theshowrunner
elif [ "$1" == "TheShowRunner1" ]; then
  	TheShowRunner1
elif [ "$1" == "TheShowRunner2" ]; then
	TheShowRunner2
elif [ "$1" == "TheShowRunner2_nologin" ]; then
	TheShowRunner2_nologin
elif [ "$1" == "iptv" ]; then
  	  iptv
elif [ "$1" == "iptvrunner" ]; then
  	  iptvrunner
elif [ "$1" == "reinstall" ]; then
  	  reinstall
elif [ "$1" == "reinstall2" ]; then
  	reinstall2
elif [ "$1" == "epg_on" ]; then
	epg_on
elif [ "$1" == "epg_off" ]; then
	epg_off
elif [ "$1" == "termuxinfo" ]; then
	termuxinfo
 elif [ "$1" == "ssh_on" ]; then
	ssh_on
elif [ "$1" == "ssh_off" ]; then
	ssh_off
 elif [ "$1" == "ssh_passwd" ]; then
	ssh_passwd
 elif [ "$1" == "write_port" ]; then
  	write_port "$2"
elif [ "$1" == "update" ]; then
   	 update
elif [ "$1" == "runcode" ]; then
    	runcode 
elif [ "$1" == "custominstall" ]; then
	custominstall "$2" "$3"
elif [ "$1" == "exitpath" ]; then
	exitpath 
else
    echo "Usage Error"
    echo "Command: .skyutils.sh $1"
    sleep 3
    exit 0
fi
