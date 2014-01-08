# formatter

A Clojure tool designed to reformat Clojure code. Very, very incomplete (and given that it's a side project, progress will probably be spradic at best).

## Usage

It's not done in the least! But, if you insist:
> lein run -i INPUT_FILE -o OUTPUT_FILE

## Structure

Each different modification (break comments over 80 lines, reformat whitespace) is a single Clojure file in the resources/extensions folder. Each file has as its last form a map. When the program is loaded, it scans the extensions folder for all the files, and uses load-file, storing the resulting maps. New extensions can be added by dropping in a clojure file. The key/value pairs used are:

* [:is-active *boolean*] - probably true
* [:description *string] - a short description of what this extension does
* [:url *string*] - a url
* [:modify-tree *(fn [[tree changes]] ...)*] - tree is a hiccup tree, and changes is a vector of strings: returns [[modified-tree updated-changes]]

I'm pretty sure that this is A Dumb Thing To Do - there's probably a better way to get a drop-in extension structure than "Use the last form in the file as a map of this form!"

## License

Copyright © 2014 Travis Moy

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
