#!/bin/bash

INPUTS=input/*

for i in $INPUTS
do
    filename=${i%.*}
    name=${filename##*/}
    output="${name}.xml"

    ./Compile.sh LittleParser $i > output/tree/$output
done
