(ns formatter.extensions.thread-first
  (:require [instaparse.core :as insta]
            [formatter.parser :as par]
			[formatter.extensions.thread-aux :as thread]))

(def ^:dynamic *min-depth* 3)
(def ^:dynamic *max-nonfirst-depth* 2)
(def ^:dynamic *do-not-nest* #{})

(defn find-nested-nodes
  "Finds nodes with a thread-last nesting level greater than or equal to n. 
  For example, for n=1 and a tree parsed from (+ 3 (* 9 2)) it would return a 
  vector of two elements containing trees representing (+3 (* 9 2)) and 
  (* 9 2)."
  [tree]
  (let [[nesting nonfirst-nesting] (thread/count-pos-nesting *do-not-nest* first tree)
        tree-nodes (filter vector? tree)
        result (mapcat find-nested-nodes tree-nodes)]
    (if (and (<= nonfirst-nesting *max-nonfirst-depth*)
             (>= nesting *min-depth*))
        (cons tree (remove #(= (second tree-nodes) %) result))
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
  {:description "thread-first adds to suggestions possible locations to use ->"
   :url ""
   :is-active true
   :process-code process-code})