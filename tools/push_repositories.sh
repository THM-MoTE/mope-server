#!/bin/sh
# pushs all moie projects to the actual branch

SERVER="../moie-server"
ATOM_PLUGIN="../moie-atom-plugin"
OMC_API="../omc-java-api"
EWS="../EnhancedWatchService"

function current_branch() {
  git branch | grep -E '^\* ' | sed 's/^\* //g'
}

cd $ATOM_PLUGIN
echo
echo "Pushing " $ATOM_PLUGIN
git push origin $(current_branch)

cd $OMC_API
echo
echo "Pushing " $OMC_API
git push origin $(current_branch)

cd $EWS
echo
echo "Pushing " $EWS
git push origin $(current_branch)

cd $SERVER
echo
echo "Pushing " $SERVER
git push origin $(current_branch)
