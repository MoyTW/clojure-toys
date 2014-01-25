(ns formatter.extensions.thread-aux
  (:require [instaparse.core :as insta]
            [formatter.parser :as par]))

(defn count-nesting
  "Counts the nesting level of the function node"
  [tree]
  (let [tree-vecs (filter #(and (vector? %) (= (first %) :Eval)) tree)]
    (if (> (count tree-vecs) 0)
        (inc (apply max (map count-nesting tree-vecs)))
        1)))

(defn count-pos-nesting 
  "Counts the nesting levels of the tree, excluding functions listed in the 
  *do-not-nest* var."
  [do-not-nest position node]
  (loop [node node n 0 nonpos-n 0]
    (let [[[_ fname] & params] (filter vector? node)]
      (if (and (= :Eval (first node))
		       (not (contains? do-not-nest fname)))
          (recur (position params)
                 (inc n) 
                 (max nonpos-n
                      (count-nesting (remove #{(position params)}
					                         params))))
          [n nonpos-n]))))