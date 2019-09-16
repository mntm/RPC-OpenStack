#!/bin/bash

pushd $(dirname $0) > /dev/null
basepath=$(pwd)
popd > /dev/null

IPADDR=$(cut -f1 -d':' <<< $2)
if [ -z "$2" ]
  then
    IPADDR="127.0.0.1"
fi

#TODO Check RMI's doc to verify if the rmi server's address is required in the java codes
java -cp "$basepath"/ns.jar:"$basepath"/shared.jar \
  -Djava.rmi.server.codebase=file:"$basepath"/shared.jar \
  -Djava.security.policy="$basepath"/policy \
  -Djava.rmi.server.hostname="$IPADDR" \
  -Drmi.object.active-port="5019" \
  ca.polymtl.inf8480.tp2.ns.NameServer $*
