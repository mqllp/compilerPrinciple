include Makefile.git

DOMAINNAME = oj.compilers.cpl.icu
ANTLR = java -jar /usr/local/lib/antlr-4.9.1-complete.jar -listener -visitor -long-messages
JAVAC = javac -g
JAVA = java

PFILE = $(shell find . -name "SysYParser.g4")
LFILE = $(shell find . -name "SysYLexer.g4")
JAVAFILE = $(shell find . -name "*.java")
ANTLRPATH = /usr/local/lib/antlr-4.9.1-complete.jar

compile: antlr
	$(call git_commit,"make")
	mkdir -p classes
	$(JAVAC) -classpath $(ANTLRPATH):. $(JAVAFILE) -d classes

run: compile
	$(JAVA) -classpath ./classes:$(ANTLRPATH) Main $(FILEPATH)

antlr: $(LFILE) $(PFILE)
	$(ANTLR) $(PFILE) $(LFILE)

test: compile
	$(call git_commit, "test")
	nohup $(JAVA) -classpath ./classes:$(ANTLRPATH) Main ./tests/test1.sysy &

clean:
	rm -f src/*.tokens
	rm -f src/*.interp
	rm -f src/SysYLexer.java src/SysYParser.java src/SysYParserBaseListener.java src/SysYParserBaseVisitor.java src/SysYParserListener.java src/SysYParserVisitor.java
	rm -rf classes
	rm -rf out
	rm -rf src/.antlr

submit: clean
	git gc
	bash submit.sh

.PHONY: compile antlr test run clean submit