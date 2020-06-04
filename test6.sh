# The first 3 files contains K&J or K&J&F or K&F which represents should be visable to keegan and joshua output file...
# I would test the first 3 first and them put 4 and 5 in there just for testing

# input is the config file, and chat is the input file (sorry, named it wired) lol

# In C it is ran with the command below

# ./ring.exe -c input1.txt -i chat1.txt -o out1.txt & ./ring.exe -c input2.txt -i chat2.txt -o out2.txt & ./ring.exe -c input3.txt -i chat3.txt -o out3.txt & ./ring.exe -c input4.txt -i chat4.txt -o out4.txt & ./ring.exe -c input5.txt -i chat5.txt -o out5.txt & wait


java ring -c test6/cfg1.txt -i test6/chat1.txt -o out1.txt &
java ring -c test6/cfg2.txt -i test6/chat2.txt -o out2.txt &
java ring -c test6/cfg3.txt -i test6/chat3.txt -o out3.txt &
java ring -c test6/cfg4.txt -i test6/chat4.txt -o out4.txt &
java ring -c test6/cfg5.txt -i test6/chat5.txt -o out5.txt &
