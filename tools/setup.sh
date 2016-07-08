#!/bin/bash
# downloads the subprojects and compiles the project

root=moie
server=moie-server

mkdir $root
cd $root
git clone git@git.thm.de:njss90/moie-server.git
git clone git@git.thm.de:njss90/omc-java-api.git
git clone https://github.com/njustus/EnhancedWatchService.git

cd $server
sbt compile

echo
echo "Projects cloned & compiled! They are at:"
echo "./"$root"/moie-server"
echo "./"$root"/omc-java-api"
echo "./"$root"/EnhancedWatchService"
