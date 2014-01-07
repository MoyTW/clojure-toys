(ns formatter.extensions.when
  (:require [instaparse.core :as insta]))

(defn replace-when-not [& args]
  (let [[pre-strs main-vec] (split-with string? args)
        [if-vec pred-vec do-vec & dne] (filter vector? main-vec)
        [do-key do-sym-vec & more] (filter #(or (vector? %) (keyword? %)) do-vec)]
    (if (and (= nil dne) 
             (= if-vec [:Symbol "if"]) 
             (= do-key :Eval) 
             (= do-sym-vec [:Symbol "do"]))
        (->> (concat pre-strs [[:Symbol "when"] " " pred-vec] more [")"])
             (cons :Eval)
             (into []))
        (into [] (cons :Eval args)))))
        
(defn fe-modify-tree [tree]
  (insta/transform {:Eval replace-when-not} tree))

{:description "(if pred (do ...)) -> (when pred ...)"
 :url "https://github.com/bbatsov/clojure-style-guide#syntax"
 :is-active true
 :modify-tree #(fe-modify-tree %)}