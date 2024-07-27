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
		pkg install jq -y
		pkg install termux-api -y
		echo "[#] Downloading Script - I"
		URL1="https://raw.githubusercontent.com/siddharthsky/CustTermux-JioTVGo/main/_BootStrap/etc/.skyutils.sh"
		curl -SL --progress-bar --retry 2 --retry-delay 2 -o "$HOME/.skyutils.sh" "$URL1" || { echo "Failed to download, Clear app data"; exit 1; }
		chmod 755 "$HOME/.skyutils.sh"
	fi
}

autoscript() {
	# Check if script exists
	if [[ -f "$HOME/.autoscript.sh" ]]; then
		return 0
	else
		echo "[#] Downloading Script - II"
		URL2="https://raw.githubusercontent.com/siddharthsky/CustTermux-JioTVGo/main/_BootStrap/etc/.autoscript_x.sh"
		curl -SL --progress-bar --retry 2 --retry-delay 2 -o "$HOME/.autoscript.sh" "$URL2" || { echo "Failed to download, Clear app data"; exit 1; }
		chmod 755 "$HOME/.autoscript.sh"
	fi
}

autoscript_xm() {
	# Check if script exists
	if [[ -f "$HOME/.autoscript_xm.sh" ]]; then
		./.autoscript_xm.sh
		return 0
	else
		echo "[#] Downloading Script - III"
		URL3="https://raw.githubusercontent.com/siddharthsky/CustTermux-JioTVGo/main/_BootStrap/etc/.autoscript_xm.sh"
		curl -SL --progress-bar --retry 2 --retry-delay 2 -o "$HOME/.autoscript_xm.sh" "$URL3" || { echo "Failed to download, Clear app data"; exit 1; }
		chmod 755 "$HOME/.autoscript_xm.sh"
   ./.autoscript_xm.sh
	fi
}


autoscript_skyutils

autoscript

autoscript_xm





