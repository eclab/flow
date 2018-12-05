.PHONY: install
indent:
	touch ${HOME}/.emacs
	find . -name "*.java" -print -exec emacs --batch --load ~/.emacs --eval='(progn (find-file "{}") (mark-whole-buffer) (setq indent-tabs-mode nil) (untabify (point-min) (point-max)) (indent-region (point-min) (point-max) nil) (save-buffer))' \;


install:
	rm -rf install/Flow.app.zip install/Flow.app install/flow.jar install/bundles install/Flow.dmg.html install/Flow.dmg.jnlp uk META-INF org
	touch /tmp/manifest.add
	rm /tmp/manifest.add
	echo "Main-Class: flow.Flow" > /tmp/manifest.add
	cd libraries ; jar -xvf coremidi4j-1.1.jar
	mv libraries/META-INF . ; mv libraries/uk .
	cd libraries ; jar -xvf json.jar
	mv libraries/org .
	jar -cvfm install/flow.jar /tmp/manifest.add `find flow -name "*.class"` `find flow -name "*.init"` `find flow -name "*.html"` `find flow -name "*.png"` `find flow -name "*.jpg"` `find flow -name "kharmonics.out"` org/ uk/ META-INF/
	rm -rf uk META-INF org
	javapackager -deploy -native dmg -srcfiles install/flow.jar -appclass flow.Flow -name Flow -outdir install -outfile Flow.dmg -v
	mv install/bundles/Flow-0.0.dmg install/Flow.dmg
