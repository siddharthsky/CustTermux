#!/bin/bash

autoboot() {
    # Function to check if the package is available
    check_package() {
        PACKAGE_NAME="com.termux.api"
        out="$(pm path $PACKAGE_NAME --user 0 2>&1 </dev/null)"
        
        # Check if the output contains the package path
        if [[ "$out" == *"$PACKAGE_NAME"* ]]; then
            echo "The package $PACKAGE_NAME is installed. Path: $out"
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
        sleep 5  # Wait for 5 seconds before checking again
    done

    echo "The package $PACKAGE_NAME is now available."
}

# Main execution
autoboot

am startservice -n com.termux/.app.TermuxService -a com.termux.service_execute
