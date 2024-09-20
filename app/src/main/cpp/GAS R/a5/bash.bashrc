if [ -x /data/data/com.termux/files/usr/libexec/termux/command-not-found ]; then
	command_not_found_handle() {
		/data/data/com.termux/files/usr/libexec/termux/command-not-found "$1"
	}
fi

PS1='\$ '


#######################################################
echo "---------------------------"
echo -e "\e[1;32mCustTERMUX - JioTV_GO\e[0m"
echo "---------------------------"
echo -e "\e[0;36mCustTermux\e[0m - SiddharthSky"
echo -e "\e[0;34mJioTVGo\e[0m - Rabilrbl"
echo "---------------------------"
#######################################################

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

autoscript_skyutils() {
    URL1="https://raw.githubusercontent.com/siddharthsky/CustTermux-JioTVGo/main/_BootStrap/etc/.skyutils.sh"
    
    get_remote_md5() {
        curl -sL "$1" | md5sum | awk '{print $1}'
    }

    get_local_md5() {
        md5sum "$1" | awk '{print $1}'
    }

    if [[ -f "$HOME/.skyutils.sh" ]]; then
        local_md5_skyutils=$(get_local_md5 "$HOME/.skyutils.sh")
        remote_md5_skyutils=$(get_remote_md5 "$URL1")

        if [[ "$local_md5_skyutils" != "$remote_md5_skyutils" ]]; then
            echo "[#] Updating .skyutils.sh to latest version"
            curl -SL --progress-bar --retry 2 --retry-delay 2 -o "$HOME/.skyutils.sh" "$URL1" || { echo "Failed to download .skyutils.sh, Clear app data"; exit 1; }
            chmod 755 "$HOME/.skyutils.sh"
			clear
        fi
    else
        pkg install termux-am -y
		SDK_VERSION=$(getprop ro.build.version.sdk)
        if [ "$SDK_VERSION" -le 23 ]; then
            chmod 400 $PREFIX/libexec/termux-am/am.apk
        fi
        echo "[#] Downloading Script - I"
        curl -SL --progress-bar --retry 2 --retry-delay 2 -o "$HOME/.skyutils.sh" "$URL1" || { echo "Failed to download, Clear app data"; exit 1; }
        chmod 755 "$HOME/.skyutils.sh"
    fi
}



update_file() {
    local local_file="$1"
    local url="$2"
	local printer="$3"

    get_remote_md5() {
        curl -sL "$url" | md5sum | awk '{print $1}'
    }

    get_local_md5() {
        md5sum "$local_file" | awk '{print $1}'
    }

    if [[ -f "$local_file" ]]; then
        local_md5=$(get_local_md5)
        remote_md5=$(get_remote_md5)

        if [[ "$local_md5" != "$remote_md5" ]]; then
            echo "[#] Updating $local_file to latest version"
            curl -SL --progress-bar --retry 2 --retry-delay 2 -o "$local_file" "$url" || { echo "Failed to download $local_file, Clear app data"; exit 1; }
            chmod 755 "$local_file"
        fi
    else
        echo "$printer"
        curl -SL --progress-bar --retry 2 --retry-delay 2 -o "$local_file" "$url" || { echo "Failed to download $local_file, Clear app data"; exit 1; }
        chmod 755 "$local_file"
    fi
}

autoscript_xz() {
    URL2="https://raw.githubusercontent.com/siddharthsky/CustTermux-JioTVGo/main/_BootStrap/etc/.termux_updates.sh"
    URL3="https://raw.githubusercontent.com/siddharthsky/CustTermux-JioTVGo/main/_BootStrap/etc/main/.autoscript_v4.4.sh"
	
    update_file "$HOME/.termux_updates.sh" "$URL2" "[#] Downloading Script - II"
    update_file "$HOME/.autoscript_xz.sh" "$URL3" " "

    ./.termux_updates.sh
    ./.autoscript_xz.sh
}




autoscript_skyutils

autoscript_xz





