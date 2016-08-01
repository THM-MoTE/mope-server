#!/bin/bash

#links the atom-plugin package into the atom editor

if which apm > /dev/null; then
    apm install
    echo "Link plugin into packages"
    apm link
else
    echo "Can't find `apm`-command; is it on your $PATH?"
fi
