#!/usr/bin/env sh

## This run-secured.sh is intended to run MIA standalone application with security authorization
## To set login and password you should pass LOGIN and PASSWORD as parameters to this script

if [ $# -eq 2 ]; then
  export SPRING_PROFILES="secured"
  export SECURE_LOGIN=$1
  export SECURE_PASSWORD=$2
  ./run.sh
else
  echo "USAGE: ./$(basename $0) login password" && exit 1
fi
