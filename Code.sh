#!/bin/bash

INPUTS=input/*

for i in $INPUTS
do
    filename=${i%.*}
    name=${filename##*/}
    output="${name}.out"
    
    ./Compile.sh CodeGenerator $i > output/$output
done
