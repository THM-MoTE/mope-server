#!/bin/bash
# downloads the subprojects and compiles the project

root=mope
server=mope-server

mkdir $root
cd $root
echo "===> Cloning server repos"
git clone https://github.com/THM-MoTE/mope-server.git
git clone https://github.com/THM-MoTE/omc-java-api.git
git clone https://github.com/THM-MoTE/EnhancedWatchService.git
git clone https://github.com/THM-MoTE/recently.git

echo
echo "===> Cloning atom plugin"
git clone https://github.com/THM-MoTE/mope-atom-plugin.git

cd "omc-java-api"
sbt compile

cd $server
sbt compile

echo
echo "===> Projects cloned & compiled! They are at:"
echo "./"$root"/mope-server"
echo "./"$root"/omc-java-api"
echo "./"$root"/EnhancedWatchService"
echo "./"$root"/mope-atom-plugin"
echo "./"$root"/recently"
echo
echo "================"
echo "===> Now installing Atom-Plugin into Atom"

cd ../mope-atom-plugin
if which apm > /dev/null; then
    apm install
    echo "Link plugin into packages"
    apm link
		echo "Plugin installed!"
else
    echo "Can't find `apm`-command; is it on your $PATH?"
fi
