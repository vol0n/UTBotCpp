#!/bin/sh
sudo apt-get update -y
apt-get install libfreetype6 fontconfig fonts-dejavu -y
./server/build/utbot server > server-log.txt 2>&1 &
cd clion-plugin
./gradlew test --info --rerun-tasks
pkill utbot
