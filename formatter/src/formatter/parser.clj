(ns formatter.parser
  (require [instaparse.core :as insta]))
(insta/set-default-output-format! :hiccup)
  
(def grammar-whitespace (insta/parser "Whitespace = #'[,\\s]+'"))
  
; Doesn't have Other Special Forms, Binding Forms
(def grammar (slurp "src/formatter/grammar.txt"))

(def par (insta/parser grammar :auto-whitespace grammar-whitespace))
(defn parser [s & args]
  (par s :unhide :content))

(defn htree-to-str [tree]
  (apply str (filter string? (flatten tree))))