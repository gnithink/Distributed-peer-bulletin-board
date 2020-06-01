JCC = "/usr/bin/javac"

JFLAGS = -g

default: Post.class Probe.class ProbeAck.class ProbeNak.class Election.class Elected.class Token.class ring.class


Post.class: Post.java
		$(JCC) $(JFLAGS) Post.java

Probe.class: Probe.java
	$(JCC) $(JFLAGS) Probe.java

ProbeAck.class: ProbeAck.java
	$(JCC) $(JFLAGS) ProbeAck.java

ProbeNak.class: ProbeNak.java
	$(JCC) $(JFLAGS) ProbeNak.java

Election.class: Election.java
	$(JCC) $(JFLAGS) Election.java

Elected.class: Elected.java
	$(JCC) $(JFLAGS) Elected.java

Token.class: Token.java
	$(JCC) $(JFLAGS) Token.java

ring.class: ring.java
	$(JCC) $(JFLAGS) ring.java

clean:
	$(RM) *.class

rmout:
	$(RM) *.txt
	lsof -i:5005 -t | xargs kill
	lsof -i:5004 -t | xargs kill
	lsof -i:5006 -t | xargs kill
	lsof -i:5008 -t | xargs kill
	lsof -i:5010 -t | xargs kill
	lsof -i:5002 -t | xargs kill


build:
	javac Post.java Probe.java ProbeAck.java ProbeNak.java Election.java Elected.java Token.java ring.java

test:	build
	(java ring -c Test2/cfg1.txt -i Test2/in1.txt -o out3456.txt) & \
	(java ring -c Test2/cfg2.txt -i Test2/in2.txt -o out3457.txt) & \
	(java ring -c Test2/cfg3.txt -i Test2/in3.txt -o out3460.txt) & \
	(java ring -c Test2/cfg4.txt -i Test2/in4.txt -o out3461.txt) & \
	wait

# target: dependencies
# 	action
# fl_undir.tab.txt fl_out.txt 1