(ns formatter.extensions.when
  (:require [instaparse.core :as insta]
            [clojure.data :as data]
            [formatter.parser :as par]))

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

(defn diff-strs [pre-tree post-tree changes]
  (reduce #(conj %1 (str "Character: " %2 " - Changed (if ... (do ...)) to (when ...)"))
          changes
          (par/diff-hiccup pre-tree post-tree)))

(defn process-code [{:keys [tree changes suggestions] :as params}]
  (let [new-tree (fe-modify-tree tree)
        new-changes (diff-strs tree new-tree changes)]
    (assoc params :tree new-tree :changes new-changes)))

(def extension
  {:description "(if pred (do ...)) -> (when pred ...)"
   :url "https://github.com/bbatsov/clojure-style-guide#syntax"
   :is-active true
   :process-code process-code})