JCC = "/usr/bin/javac"

JFLAGS = -g

p1 = lsof -t -i:5005
p2 = lsof -t -i:5008
p3 = lsof -t -i:5010
p4 = lsof -t -i:5002
p5 = lsof -t -i:5004
p6 = lsof -t -i:5006

p7 = lsof -t -i:3451
p8 = lsof -t -i:3450
p9 = lsof -t -i:3456
p10 = lsof -t -i:3458
p11 = lsof -t -i:3461
p12 = lsof -t -i:3455
p13 = lsof -t -i:3459


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
	# $(RM) *.txt

rmout:
	$(RM) *.txt
	
	# lsof -i:5005 -t | xargs kill
	# lsof -i:5004 -t | xargs kill
	# lsof -i:5006 -t | xargs kill
	# lsof -i:5008 -t | xargs kill
	# lsof -i:5010 -t | xargs kill
	# lsof -i:5002 -t | xargs kill


	# lsof -i:3451 -t | xargs kill
	# lsof -i:3450 -t | xargs kill
	# lsof -i:3456 -t | xargs kill
	# lsof -i:3458 -t | xargs kill
	# lsof -i:3461 -t | xargs kill
	# lsof -i:3455 -t | xargs kill
	# lsof -i:3459 -t | xargs kill

	# kill -9 $(p1)
	# kill -9 $(p2)
	# kill -9 $(p3)
	# kill -9 $(p4)
	# kill -9 $(p5)
	# kill -9 $(p6)
	

	# kill -9 $(p7)
	# kill -9 $(p8)
	# kill -9 $(p9)
	# kill -9 $(p10)
	# kill -9 $(p11)
	# kill -9 $(p12)
	# kill -9 $(p13)
	




# target: dependencies
# 	action
# fl_undir.tab.txt fl_out.txt 1