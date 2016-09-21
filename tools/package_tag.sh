#!/bin/sh
# packages the given tag as a jar and moves it in the root directory of the project

tag=$1
git checkout $tag
sbt ";clean;compile;assembly"
mv target/scala-2.11/*.jar ./
