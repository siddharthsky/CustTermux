#!/bin/bash

RED='\033[0;31m'      # Red
GREEN='\033[0;32m'    # Green
YELLOW='\033[0;33m'   # Yellow
NC='\033[0m'          # No Color

# The fixed part of the text
prefix="##### "
suffix=" #####"

# The word to change color
word="ひらめき"

# Array of colors
colors=($RED $GREEN $YELLOW)

# Loop through each character of the word
echo -n "$prefix"
for (( i=0; i<${#word}; i++ )); do
  # Use the modulus operator to cycle through colors
  color=${colors[$((i % ${#colors[@]}))]}
  
  # Print the character with the selected color
  echo -ne "${color}${word:$i:1}${NC}"
done
echo "$suffix"





# echo "##################### ひらめき ############################"
#echo -e "${RED}WARNING:${NC} Please update the app to the latest version whenever it becomes available."
#echo -e "${RED}NOTE:${NC} To be on the safer side, we are renaming the repositories. Stay tuned for the next updates."
#echo -e "${RED}IMPORTANT:${NC} After ${GREEN}01.09.2024${NC}, you will not be able to install older versions of CustTermux."

#echo ""

# echo -e "${RED}NOTICE:${NC} ${NC}To avoid potential copyright issues with Jio,${NC}"
# echo -e "${YELLOW}we will be renaming our GitHub repository.${NC}"
# echo -e "${GREEN}Update will be provided soon.${NC}"
