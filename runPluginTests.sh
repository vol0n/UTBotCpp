#!/bin/sh
sudo apt-get update -y
apt-get install libfreetype6 fontconfig fonts-dejavu -y

set -e

#Starting the X-server
export DISPLAY=':99.0'
Xvfb :99 -screen 0 1024x768x24 > /dev/null 2>&1 &

./server/build/utbot server > server-log.txt 2>&1 &

cd clion-plugin
./gradlew test --info --rerun-tasks
pkill utbot
