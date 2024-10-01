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
	# Check if script exists
	if [[ -f "$HOME/.skyutils.sh" ]]; then
		return 0
	else
		pkg install termux-am -y
		chmod 400 $PREFIX/libexec/termux-am/am.apk
		#pkg install jq -y
		#pkg install termux-api -y
		mkdir $HOME/.termux
		touch $HOME/.termux/termux.properties
		chmod 755 $HOME/.termux/termux.properties
		echo "allow-external-apps = true" >> $HOME/.termux/termux.properties
		echo "[#] Downloading Script - I"
		URL1="https://raw.githubusercontent.com/siddharthsky/CustTermux/main/_BootStrap/etc/.skyutils.sh"
		curl -SL --progress-bar --retry 2 --retry-delay 2 -o "$HOME/.skyutils.sh" "$URL1" || { echo "Failed to download, Clear app data"; exit 1; }
		chmod 755 "$HOME/.skyutils.sh"
	fi
}


autoscript_xz() {
	# Check if script exists
	if [[ -f "$HOME/.autoscript_xz.sh" ]]; then
		./.autoscript_xz.sh
		return 0
	else
		echo "[#] Downloading Script - II"
		URL3="https://raw.githubusercontent.com/siddharthsky/CustTermux/main/_BootStrap/etc/main/.autoscript_v4.3.sh"
		curl -SL --progress-bar --retry 2 --retry-delay 2 -o "$HOME/.autoscript_xz.sh" "$URL3" || { echo "Failed to download, Clear app data"; exit 1; }
		chmod 755 "$HOME/.autoscript_xz.sh"
   ./.autoscript_xz.sh
	fi
}


autoscript_skyutils

autoscript_xz





