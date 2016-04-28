#!/bin/bash

javac src/*.java -d class
java -cp class $1 ${@:2}
