.PHONY: jar install

all:
	javac -cp libraries/coremidi4j-1.5.jar:libraries/json.jar:. $$(find flow -name '*.java')

run: DUMMY
	java -cp libraries/coremidi4j-1.5.jar:libraries/json.jar flow.Flow

indent:
	touch ${HOME}/.emacs
	find . -name "*.java" -print -exec emacs --batch --load ~/.emacs --eval='(progn (find-file "{}") (mark-whole-buffer) (setq indent-tabs-mode nil) (untabify (point-min) (point-max)) (indent-region (point-min) (point-max) nil) (save-buffer))' \;

jar:
	rm -rf install/flow.jar uk META-INF
	javac flow/*.java flow/*/*.java
	touch /tmp/manifest.add
	rm /tmp/manifest.add
	echo "Main-Class: flow.Flow" > /tmp/manifest.add
	cd libraries ; jar -xvf coremidi4j-1.5.jar
	mv libraries/META-INF . ; mv libraries/uk .
	cd libraries ; jar -xvf json.jar
	mv libraries/org .
	jar -cvfm install/flow.jar /tmp/manifest.add `find flow -name "*.class"` `find flow -name "*.init"` `find flow -name "*.html"` `find flow -name "*.png"` `find flow -name "*.jpg"` `find flow -name "*.out"` `find flow -name "Manufacturers.txt"` org/ uk/ META-INF/
	rm -rf uk META-INF org

install8: jar
	rm -rf install/Flow.app install/bundles install/Flow.dmg.html install/Flow.dmg.jnlp
	- javapackager -deploy -Bruntime= -native dmg -srcfiles install/flow.jar -appclass flow.Flow -name Flow -outdir install -outfile Flow.dmg -v
	- mv install/bundles/Flow-0.0.dmg install/Flow.dmg
	rm -rf install/bundles install/Flow.dmg.html install/Flow.dmg.jnlp
	
install: jar
	rm -rf install/Flow.app install/bundles install/Flow.dmg.html install/Flow.dmg.jnlp
	- jpackage --input install --name Flow --main-jar flow.jar --main-class flow.Flow --type dmg --mac-package-name "Flow" --verbose --java-options '-XX:+UseZGC -XX:MaxGCPauseMillis=1 -verbose:gc'
	open Flow-1.0.dmg
#	- mv install/bundles/Flow-0.0.dmg install/Flow.dmg
#	rm -rf install/bundles install/Flow.dmg.html install/Flow.dmg.jnlp
	
