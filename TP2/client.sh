#!/bin/bash

pushd $(dirname $0) > /dev/null
basepath=$(pwd)
popd > /dev/null


java -cp "$basepath"/client.jar:"$basepath"/shared.jar \
    -Djava.security.policy="$basepath"/policy \
    -Drmi.object.active-port="5027" \
    ca.polymtl.inf8480.tp2.client.Client $*
