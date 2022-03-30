#!/bin/bash

sudo apt-get update -y
apt-get install libfreetype6 fontconfig fonts-dejavu -y

java --version
# enable needed envs for server
source docker/building_dependencies/runtime_env.sh

# look at include paths
cpp -v /dev/null /dev/null
java --version

set -e

#Starting the X-server
export DISPLAY=':99.0'
Xvfb :99 -screen 0 1024x768x24 > /dev/null 2>&1 &

./server/build/utbot server > server-log.txt 2>&1 &

# look at include paths
cpp -v /dev/null /dev/null

cd clion-plugin
./gradlew test --info --rerun-tasks
pkill utbot
