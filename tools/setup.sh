#!/bin/bash
# downloads the subprojects and compiles the project

root=moie
server=moie-server

mkdir $root
cd $root
echo "===> Cloning server repos"
git clone git@git.thm.de:njss90/moie-server.git
git clone git@git.thm.de:njss90/omc-java-api.git
git clone https://github.com/njustus/EnhancedWatchService.git

echo
echo "===> Cloning atom plugin"
git clone "git@git.thm.de:njss90/moie-atom-plugin.git"

cd $server
sbt compile

echo
echo "===> Projects cloned & compiled! They are at:"
echo "./"$root"/moie-server"
echo "./"$root"/omc-java-api"
echo "./"$root"/EnhancedWatchService"
echo "./"$root"/moie-atom-plugin"
echo
echo "================"
echo "===> Now installing Atom-Plugin into Atom"

cd ../moie-atom-plugin
if which apm > /dev/null; then
    apm install
    echo "Link plugin into packages"
    apm link
		echo "Plugin installed!"
else
    echo "Can't find `apm`-command; is it on your $PATH?"
fi
