# comment-breaker

Breaks long comments into n-length lines.

Targets comments with two semicolons. Will indent the head line of the broken
comment by two spaces.

For example,

	;; This is a very long comment that goes over eighty characters! It's Not Ideal At All!

will become

	;;   This is a very long comment that goes ever eighty characters! It's Not
	;; Ideal At All.

## Installation

Download from http://example.com/FIXME.

## Usage

Invoke from command-line.

    $ java -jar comment-breaker-0.1.0-standalone.jar -i FILE [args]

## Options

    -i FILE, --in-file 
		The file to read the input from
	
	-o FILE, --out-file
		The file to write the output to. If no file is defined, will be
	in-name + "-broken" + in-extension
	
	-l NUM, --length
		The desired line length

## License

Copyright © 2013 Travis Moy

Distributed under the MIT License (go look at the license file if you want).
