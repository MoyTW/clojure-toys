(ns wysitutwyg.markov.builder
  (:require [clojure.data.json :as json]))

;; Fixes seed for reproducability
(def ^:dynamic ^java.util.Random *rnd* (java.util.Random. 1))
(defn get-rand [x]
  (.nextInt *rnd* x))

(def map-loc "resources/public/corpora/loremipsum/1.json")

(def datamap
  (let [initial (into {} 
                   (for [[k v] (json/read-str (slurp map-loc))]
                     [(keyword k) v]))]
    (-> initial
        (update-in [:end] #(into #{} %))
        (update-in [:start] #(apply hash-map (apply concat %)))
        (update-in [:counts] #(apply hash-map (apply concat %))))))
        
(defn pick-word
  "Takes a map of counts in the form of {'sad' 2, 'mad' 3} (values from the 
  n-gram map) and picks a word, by the ratio of the counts."
  [words]
  (if-let [word-seq (seq words)]
    (let [keys (vec (map first word-seq))
          counts (vec (map second word-seq))
          r (get-rand (reduce + counts))]
      (loop [i 0 sum 0]
        (if (< r (+ (counts i) sum))
            (nth keys i)
            (recur (inc i) (+ (counts i) sum)))))))

; ((:counts datamap) ["a"])
; (pick-word (:start datamap))
; (pick-word ((datamap :counts) ["a"]))

;;;   Our aim is: "Pick the first generated sentence of size n which has as 
;;; its last member an end node. If there is no possible sentence of length n
;;; ending in an end node, pick the last selected."
;;;   The current implementation uses a straight BFS.

;; Base case: (depth=n) OR (no children return !nil)

(defn pick-words
  "Returns a lazy sequence of the next states from the start state, generated
  by repeatedly picking words."
  [counts]
  (letfn [(step [candidates]
            (if-let [w (pick-word candidates)]
              (cons w (step (dissoc candidates w)))
              nil))]
    (lazy-seq (step counts))))

(prn (pick-words ((datamap :counts) ["a"])))
(prn (pick-words ((datamap :counts) ["a"])))
(prn (pick-words ((datamap :counts) ["a"])))
(prn (pick-words (datamap :start)))

(defn find-first-end
  "Returns the first end node following the specified state, nil if none."
  [state {:keys [counts end] :as datamap}]
  (first (filter end (pick-words (counts state)))))

(assert (find-first-end ["accepted"] datamap))
(assert (not (find-first-end ["as"] datamap)))

(defn words-to-states
  "Takes a state and a collection of words, and maps it to the next set of
  states."
  [state words]
  (map #(conj (vec (rest state)) %) words))

;; TODO: Is this dfs code needlessly complex/confusing? Probably. Refactor?
(declare dfs)

(defn dfs-step
  "Maps dfs to the words (sideways expansion), mutually recursive with dfs.
  Returns lazy sequence of state/nil values."
  [state datamap n depth words]
  (->> (words-to-states state words)
       (map #(dfs % datamap n (inc depth)))
       (filter #(not= nil %))
       (first)))

;; TODO: It won't work on 2-grams, 3-grams, etc. Only singles.
(defn dfs
  "Runs the dfs (downwards expansion), call this to start."
  [state {:keys [counts] :as datamap} n depth]
  (if (= depth n)
      (if-let [end-state (find-first-end state datamap)]
        (cons state (vector (vector end-state)))) ; hahaha that's silly
      (let [words (pick-words (counts state))]
        (if-let [result (dfs-step state datamap n depth words)]
          (cons state result)))))

;(defn dfs-begin
;  [{:keys [start counts] :as datamap} n depth]

      
(prn "With No:" (dfs ["No"] datamap 5 0))
; "No one who chooses to distinguish." - how to get it to return this?
(prn "With No:" (dfs ["No"] datamap 2 0))
;;; What do we do if it's nil (cannot find any sentences of that length)?
;;; We could do:
;;;   * Run it with a different start node, but that may just delay it. So, 
;;;     that doesn't actually solve our problem!
;;;   * Do a single-path dfs to the targeted depth.

#_(defn lazy-chain
  "Given a starting state, returns a lazy sequence of generated words until it
  reaches an end state."
  [start-state counts]
  (if-let [next-word (pick-word (counts (map #(apply str %) start-state)))]
    (cons (apply str next-word)
          (lazy-seq (lazy-chain (conj (vec (rest start-state))
                                      next-word)
                                counts)))))

#_(defn generate-text
  "Given a starting state as a string, generates n further words from the given
  count graph. Ends once n words have been generated, or when a terminal node
  has been reached."
  [state n counts]
  (->> (reverse state)
       (map #(apply str %))
       (apply conj (->> counts
                        (lazy-chain (map seq state))
                        (take (dec n))))
       (interpose \space)
       (apply str)))