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
	lsof -i:3460 -t | xargs kill
	lsof -i:3456 -t | xargs kill



# target: dependencies
# 	action
# fl_undir.tab.txt fl_out.txt 1