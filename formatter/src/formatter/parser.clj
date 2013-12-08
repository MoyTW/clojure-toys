(ns formatter.parser
  (require [instaparse.core :as insta]))
;(insta/set-default-output-format! :enlive)
(insta/set-default-output-format! :hiccup)
  
(def grammar-whitespace
  (insta/parser
    "Whitespace = #'[,\\s]+'"))
  
; Doesn't have Other Special Forms, Binding Forms
(def grammar (slurp "src/formatter/grammar.txt"))
(def str-str (slurp "src/formatter/string.txt"))

(def par (insta/parser grammar :auto-whitespace grammar-whitespace))
(defn parser [s & args]
  (par s :unhide :content))

(def string-simple
"(cons 4 [1 23])")

(def string-bool
"(if false false true)")

(def string-int
"65 13 199 5r13 (!set zoo 9)")

(def string-quotes
"(str\"Zeus\"\"Hera\")")

(def string-reify
"(reify FormatterExtension
  (is-active [this] true)
  (modify-string [this x] x))")
  
(def string-40
"(fn custom-interpose [sep, in-seq]
  (butlast (flatten
   (map (fn [in, sep] [in sep]) in-seq (take (count in-seq) (repeat sep))))))")

(def string-when-not 
"(when (not pred )
       foo
       bar)")
(def tree-when-not (parser string-when-not :unhide :content))
(def t-node (nth tree-when-not 1))

(defn htree-to-str [tree]
  (apply str (filter string? (flatten tree))))