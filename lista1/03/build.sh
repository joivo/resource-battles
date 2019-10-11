#!/bin/sh

if [[ "$#" -ne 1 ]]; then
  echo "It required specify how many processes and threads are run "
  echo "usage:\n $0 <n_agents>\n"
  exit 1
fi

n_threads=$1

gcc threads.c -lpthread -o threads
./threads

gcc processes.c -o proc
./proc 