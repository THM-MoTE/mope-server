#!/bin/bash

force=$1
SERVER="../moie-server"
OMC_API="../omc-java-api"
EWS="../EnhancedWatchService"

function compileSbt() {
  if [ $force == "true" ]
  then
    sbt ";clean;compile"
  else
    sbt compile
  fi
}
function compileGradle() {
  if [ $force == "true" ]
  then
    gradle clean compileJava
  else
    gradle compileJava
  fi
}

echo "compiling " $EWS
cd $EWS
compileSbt
compileGradle

echo
echo "compiling " $OMC_API
cd $OMC_API
compileSbt
compileGradle

echo
echo "compiling " $SERVER
cd $SERVER
compileSbt
