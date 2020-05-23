#!/bin/bash


java ring -c Test2/cfg1.txt -i Test2/in1.txt -o out1.txt &
java ring -c Test2/cfg2.txt -i Test2/in2.txt -o out2.txt &

