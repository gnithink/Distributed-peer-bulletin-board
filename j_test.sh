#!/bin/bash
java ring -c j_test/4config.txt -i j_test/4input.txt -o 4out.txt &
java ring -c j_test/6config.txt -i j_test/6input.txt -o 6out.txt &
java ring -c j_test/8config.txt -i j_test/8input.txt -o 8out.txt &
java ring -c j_test/10config.txt -i j_test/10input.txt -o 10out.txt &
java ring -c j_test/2config.txt -i j_test/2input.txt -o 2out.txt &


