#!/bin/bash

# enable needed envs for server
source docker/building_dependencies/runtime_env.sh

sudo apt-get update -y

# install font config. Without it java.awt will throw, and ide will exit.
apt-get install libfreetype6 fontconfig fonts-dejavu -y

set -e

# export GRPC_VERBOSITY=debug
# export GRPC_TRACE=api
./server/build/utbot server &> server_output.log &

cd clion-plugin
./gradlew test --info --rerun-tasks


