#!/usr/bin/env bash

# This is a BASH equivalent of pipeline-timeout-prekill.groovy
# It traps the kill signal and runs a pre-kill script before killing the child process
# Note that the trick here is to background and wait.
# Running the process in the foreground will cause the signal to kill it.
# code adapted from https://unix.stackexchange.com/a/146770/199293

# for simplicity, args are:
# $1: command to run
# $2: command to run if being killed
# 
# eg:
#   bash pipeline-timeout-prekill.sh "./gradlew slow task" "killall -3 java"

PREKILL="$2"  #save for _term()

_term() {
  echo "Caught SIGTERM signal!"
  echo "Performing cleanup now using command: $PREKILL"
  $PREKILL
  echo "Cleanup completed, killing process"
  kill -TERM "$child" 2>/dev/null
}

trap _term SIGTERM

echo "Starting main subprocess...";
# Start some slow process, could just be sleep...
$1 &

child=$!
wait "$child"
