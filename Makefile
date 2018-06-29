EMACS_PATH = "/Applications/Emacs.app/Contents/MacOS/Emacs"

.PHONY: tangle
tangle:
	find ./src/music_classifier -name '*.org' | while read line; do ${EMACS_PATH} --batch -l org --eval "(org-babel-tangle-file \"$$line\")"; done 

server: tangle
	lein clean && lein ring server

release: tangle
	lein clean && lein uberjar

repl: tangle
	lein clean && lein repl

run: tangle
	lein run

