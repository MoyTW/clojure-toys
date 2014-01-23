(ns formatter.extensions.thread-first
  (:require [instaparse.core :as insta]
            [formatter.parser :as par]))

(def ^:dynamic *min-depth* 3)
(def ^:dynamic *max-nonfirst-depth* 2)
(def ^:dynamic *do-not-nest* #{})

(defn count-nonfirst-nesting
  "Counts the nesting level of the function node"
  [tree]
  (let [tree-vecs (filter #(and (vector? %) (= (first %) :Eval)) tree)]
    (if (> (count tree-vecs) 0)
        (inc (apply max (map count-nonfirst-nesting tree-vecs)))
        1)))

(defn count-first-nesting 
  "Counts the nesting levels of the tree, excluding functions listed in the 
  *do-not-nest* var."
  [node]
  (loop [node node n 0 nonfirst-n 0]
    (let [node-vecs (filter vector? node)]    
      (cond
        (contains? *do-not-nest* (second (first node-vecs)))
          [n nonfirst-n]
        (and (= :Eval (first node)))
          (recur (second node-vecs)
                 (inc n) 
                 (max nonfirst-n
                      (count-nonfirst-nesting (rest (rest node-vecs)))))
        :else
          [n nonfirst-n]))))

(defn find-nested-nodes
  "Finds nodes with a thread-last nesting level greater than or equal to n. 
  For example, for n=1 and a tree parsed from (+ 3 (* 9 2)) it would return a 
  vector of two elements containing trees representing (+3 (* 9 2)) and 
  (* 9 2)."
  [tree]
  (let [[nesting nonfirst-nesting] (count-first-nesting tree)
        tree-nodes (filter vector? tree)
        result (mapcat find-nested-nodes tree-nodes)]
    (if (and (<= nonfirst-nesting *max-nonfirst-depth*)
             (>= nesting *min-depth*))
        (cons tree (remove #(= (second tree-nodes) %) result))
        (mapcat find-nested-nodes tree-nodes))))

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
       (map clojure.string/trim)
       (reduce conj suggestions)
       (assoc params :suggestions)))

(def extension
  {:description "thread-first adds to suggestions possible locations to use ->"
   :url ""
   :is-active true
   :process-code process-code})