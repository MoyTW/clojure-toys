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
  [{:keys [do-not-nest thread-pred]} node]
  (loop [node node n 0 nonpos-n 0]
    (let [[[_ fname] & params] (filter vector? node)]
      (if (and (= :Eval (first node))
          (not (contains? do-not-nest fname)))
          (recur (thread-pred params)
                 (inc n) 
                 (max nonpos-n
                      (count-nesting (remove #{(thread-pred params)}
                                             params))))
          [n nonpos-n]))))

(defn find-nested-nodes
  "Finds nodes with a thread-last nesting level greater than or equal to n. 
  For example, for n=1 and a tree parsed from (+ 3 (* 9 2)) it would return a 
  vector of two elements containing trees representing (+3 (* 9 2)) and 
  (* 9 2)."
  [env-vars-map tree]
  (let [{:keys [min-depth max-branch-depth thread-pred]} env-vars-map
        [nesting nonpos-nesting] (count-pos-nesting env-vars-map tree)
        [_ & params :as tree-nodes] (filter vector? tree)
        result (mapcat #(find-nested-nodes env-vars-map %) tree-nodes)]
    (if (and (<= nonpos-nesting max-branch-depth)
             (>= nesting min-depth))
        (cons tree (remove #{(thread-pred params)} result))
        result)))

(defn node-to-suggestion
  "Takes a node and turns it into a suggestion map."
  [node]
  (->> node
       (par/htree-to-str)
       (clojure.string/trim)
       (hash-map :code)))

(defn process-code [threading-params {:keys [tree suggestions] :as params}]
  (->> (find-nested-nodes threading-params tree)
       (map node-to-suggestion)
       (reduce conj suggestions)
       (assoc params :suggestions)))