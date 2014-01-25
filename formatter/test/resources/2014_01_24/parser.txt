(ns formatter.parser
  (require [instaparse.core :as insta]
           [clj-diff.core :as cdiff]))
(insta/set-default-output-format! :hiccup)
  
(def grammar-whitespace (insta/parser "Whitespace = #'[,\\s]+'"))
  
; Doesn't have Other Special Forms, Binding Forms
(def grammar (slurp (clojure.java.io/resource "grammar.txt")))

(def par (insta/parser grammar :auto-whitespace grammar-whitespace))
(defn parser [s & args]
  (par s :unhide :content))

(defn htree-to-str [tree]
  (apply str (filter string? (flatten tree))))

(defn diff-hiccup [left right]
  (let [diff-results (cdiff/diff (htree-to-str left) (htree-to-str right))
        char-diff-pos (map first (:+ diff-results))]
    char-diff-pos))