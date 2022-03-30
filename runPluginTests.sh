#!/bin/bash

# enable needed envs for server
source docker/building_dependencies/runtime_env.sh

sudo apt-get update -y

# install font config. Without it java.awt will throw, and ide will exit.
# Strange that it is not installed by default.
apt-get install libfreetype6 fontconfig fonts-dejavu -y

#install java zulu 11 distribution
sudo apt update -y
sudo apt install dirmngr --install-recommends -y
sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys 0xB1998361219BD9C9
sudo apt-add-repository 'deb http://repos.azulsystems.com/ubuntu stable main' -y

sudo apt update -y
sudo apt install zulu-11 -y


echo "JAVA VERSION"
java --version

echo "WHICH JAVA"
which java


# look at include paths
echo "INCLUDE PATHS"
cpp -v /dev/null /dev/null

set -e

#Starting the X-server
export DISPLAY=':99.0'
Xvfb :99 -screen 0 1024x768x24 > /dev/null 2>&1 &

./server/build/utbot server > server-log.txt 2>&1 &

# look at include paths
echo "INCLUDE PATHS"
cpp -v /dev/null /dev/null

cd clion-plugin
./gradlew test --info --rerun-tasks
pkill utbot
