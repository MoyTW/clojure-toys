(ns formatter.extensions.when
  (:require [instaparse.core :as insta]
            [clojure.data :as data]
            [formatter.parser :as par]))

(defn replace-nodes
  "Replaces nodes within a (if pred (do ... )) node. Changes the [if] to a 
  [when] and pulls the body of the [do] up one level on the tree."
  [if-node do-node node]
  (letfn [(compare-node [node]
            (condp = node
              if-node [[:Symbol "when"]]
              do-node (->> (rest node)
                      (drop-while string?)
                      (remove #{"(" ")" [:Symbol "do"]})
                      (into []))
                [node]))]
    (mapcat compare-node node)))

(defn replace-when-not [& args]
  (let [[if-node pred-node do-node & dne] (filter vector? args)
        do-vecs (filter vector? do-node)]
    (if (and (= nil dne)
             (= if-node [:Symbol "if"])
             (= (first do-node) :Eval)
             (= (first do-vecs) [:Symbol "do"]))
        (->> args
             (replace-nodes if-node do-node)
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