#!/bin/bash

javac src/*.java -d class
java -ea -cp class $1 ${@:2}
