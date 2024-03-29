#!/bin/bash

pushd $(dirname $0) > /dev/null
basepath=$(pwd)
popd > /dev/null


IPADDR=$(cut -f1 -d':' <<< $3)
if [ -z "$3" ]
  then
    IPADDR="127.0.0.1"
fi

java -cp "$basepath"/lb.jar:"$basepath"/shared.jar \
  -Djava.rmi.server.codebase=file:"$basepath"/shared.jar \
  -Djava.security.policy="$basepath"/policy \
  -Djava.rmi.server.hostname="$IPADDR" \
  -Drmi.object.active-port="5037" \
  ca.polymtl.inf8480.tp2.lb.LoadBalancer $*
