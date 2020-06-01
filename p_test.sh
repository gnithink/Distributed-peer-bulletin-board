#!/bin/bash
java ring -c p_test/4config.txt -i p_test/4input.txt -o 4out.txt &
java ring -c p_test/6config.txt -i p_test/6input.txt -o 6out.txt &
java ring -c p_test/8config.txt -i p_test/8input.txt -o 8out.txt &
java ring -c p_test/10config.txt -i p_test/10input.txt -o 10out.txt &

