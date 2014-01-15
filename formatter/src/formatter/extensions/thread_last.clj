(ns formatter.extensions.thread-last
  (:require [instaparse.core :as insta]
            [formatter.parser :as par]))

(def ^:dynamic min-depth 3)
(def ^:dynamic do-not-nest
  #{"defn" "loop" "let" "if" "and" "or" "fn"})

(defn count-nesting 
  "Counts the nesting levels of the tree, excluding functions listed in the 
  do-not-nest var."
  [node]
  (loop [node node n 0]
    (let [node-vecs (filter #(or (vector? %) (= :Eval %)) node)]
      (cond
        (contains? do-not-nest (second (second node-vecs)))
          n
        (and (= :Eval (first node-vecs)) (vector? (last node-vecs)))
          (recur (last node-vecs) (inc n))
        :else
          n))))

(defn find-nested-nodes
  "Finds nodes with a thread-last nesting level greater than or equal to n. 
  For example, for n=1 and a tree parsed from (+ 3 (* 9 2)) it would return a 
  vector of two elements containing trees representing (+3 (* 9 2)) and 
  (* 9 2)."
  [tree]
  (if (sequential? tree)
      (if (>= (count-nesting tree) min-depth)
          (cons tree (mapcat #(find-nested-nodes %) (next tree)))
          (mapcat #(find-nested-nodes %) (next tree)))
      nil))

(defn nodes-to-strings
  "Takes a vector or nodes and returns a vector of strings."
  [nodes]
  (map #(->> %
             (flatten)
             (filter string?)
             (apply str))
        nodes))

(defn process-code [{:keys [tree suggestions] :as params}]
  (->> (find-nested-nodes tree)
       (nodes-to-strings)
       (reduce conj suggestions)
       (assoc params :suggestions)))

(def extension
  {:description "thread-last adds to suggestions possible locations to use ->>"
   :url ""
   :is-active true
   :process-code process-code})