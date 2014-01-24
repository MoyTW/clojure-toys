(ns formatter.extensions.thread-last
  (:require [instaparse.core :as insta]
            [formatter.parser :as par]))

(def ^:dynamic *min-depth* 3)
(def ^:dynamic *max-nonlast-depth* 2)
(def ^:dynamic *do-not-nest*
  #{"defn" "loop" "let" "if" "and" "or" "fn" "deftest" "is" "testing"})

(defn count-nonlast-nesting
  "Counts the nesting level of the function node"
  [tree]
  (let [tree-vecs (filter #(and (vector? %) (= (first %) :Eval)) tree)]
    (if (> (count tree-vecs) 0)
        (inc (apply max (map count-nonlast-nesting tree-vecs)))
        1)))

(defn count-last-nesting 
  "Counts the nesting levels of the tree, excluding functions listed in the 
  *do-not-nest* var."
  [node]
  (loop [node node n 0 nonlast-n 0]
    (let [node-vecs (filter vector? node)]
      (cond
        (contains? *do-not-nest* (second (first node-vecs)))
          [n nonlast-n]
        (and (= :Eval (first node)))
          (recur (last node-vecs) 
                 (inc n) 
                 (max nonlast-n
                      (count-nonlast-nesting (butlast node-vecs))))
        :else
          [n nonlast-n]))))

(defn find-nested-nodes
  "Finds nodes with a thread-last nesting level greater than or equal to n. 
  For example, for n=1 and a tree parsed from (+ 3 (* 9 2)) it would return a 
  vector of two elements containing trees representing (+3 (* 9 2)) and 
  (* 9 2)."
  [tree]
  (let [[nesting nonlast-nesting] (count-last-nesting tree)
        tree-nodes (filter vector? tree)
        result (mapcat find-nested-nodes tree-nodes)]
    (if (and (<= nonlast-nesting *max-nonlast-depth*)
             (>= nesting *min-depth*))
        (cons tree (remove #(= (last tree-nodes) %) result))
        (mapcat find-nested-nodes tree-nodes))))

(defn node-to-suggestion
  "Takes a node and turns it into a suggestion map."
  [node]
  (->> node
       (par/htree-to-str)
	   (clojure.string/trim)
	   (hash-map :code)))

(defn process-code [{:keys [tree suggestions] :as params}]
  (->> (find-nested-nodes tree)
       (map node-to-suggestion)
       (reduce conj suggestions)
       (assoc params :suggestions)))

(def extension
  {:description "thread-last adds to suggestions possible locations to use ->>"
   :url ""
   :is-active true
   :process-code process-code})