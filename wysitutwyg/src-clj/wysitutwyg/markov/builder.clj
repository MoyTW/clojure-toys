(ns wysitutwyg.markov.builder
  (:require [clojure.data.json :as json]))

;; Fixes seed for reproducability
(def ^:dynamic ^java.util.Random *rnd* (java.util.Random. 1))
(defn get-rand [x]
  (.nextInt *rnd* x))

 (def map-loc "resources/public/corpora/loremipsum/1.json")

(defn read-into-datamap
  [slurpable]
  (let [initial (into {}
                  (for [[k v] (json/read-str (slurp slurpable))]
                    [(keyword k) v]))]
    (-> initial
        (update-in [:end] #(into #{} %))
        (update-in [:start] #(apply hash-map (apply concat %)))
        (update-in [:counts] #(apply hash-map (apply concat %))))))

(def datamap (read-into-datamap map-loc))
(def counts (:counts datamap))
        
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

; (assert (find-first-end ["accepted"] datamap))
; (assert (not (find-first-end ["as"] datamap)))

(defn words-to-states
  "Takes a state and a collection of words, and maps it to the next set of
  states."
  [state words]
  (map #(conj (vec (rest state)) %) words))

;; TODO: Is this dfs code needlessly complex/confusing? Probably. Refactor?
(declare dfs-vert)

(defn dfs-hori
  "Maps dfs to the words (sideways expansion), mutually recursive with dfs.
  Returns lazy sequence of state/nil values."
  [state {:keys [end] :as datamap} n depth words]
  ; (prn "Hori - Depth: " depth " State: " state)
  (->> (words-to-states state words)
       (remove #(end (last %)))
       (map #(dfs-vert % datamap n (inc depth)))
       (remove nil?)
       (first)))

;; TODO: It won't work on 2-grams, 3-grams, etc. Only singles.
;; TODO: Ugly and unhelpfully obtuse.
(defn dfs-vert
  "Runs the dfs (downwards expansion), call this to start."
  [state {:keys [counts] :as datamap} n depth]
  ; (prn "Vert - Depth: " depth " State: " state)
  ; (prn state result depth)
  (if (= depth (dec n))
      (if-let [end-state (find-first-end state datamap)]
        (cons state (vector (vector end-state)))) ; This is pretty silly.
      (if-let [result 
                 (dfs-hori state datamap n depth (pick-words (counts state)))]
        (cons state result))))
          
(prn "With No:" (dfs-vert ["No"] datamap 5 0))
; "No one who chooses to distinguish." - how to get it to return this?
(prn "With No:" (dfs-vert ["No"] datamap 2 0))
;;; What do we do if it's nil (cannot find any sentences of that length)?
;;; We could do:
;;;   * Run it with a different start node, but that may just delay it. So, 
;;;     that doesn't actually solve our problem!
;;;   * Do a single-path dfs to the targeted depth.

(defn dfs-begin
  [{:keys [start counts] :as datamap} n]
  {:pre [(pos? n)]}
  (->> (pick-words start)
       (map #(dfs-vert % datamap n 0))
       (remove nil?)
       (first)))

(defn dive
  [state {:keys [counts] :as datamap} n depth]
  (if (= depth (dec n))
      [state]
      (let [result [(pick-word (counts state))]]
        (cons state (dive result datamap n (inc depth))))))

(defn dfs-single
  [{:keys [start counts] :as datamap} n]
  {:pre [(pos? n)]}
  (dive (pick-word start) datamap n 0))

;; TODO: Assumes single tuples.
;; TODO: While this handles multiple sentences, the fact that it generates
;; them is a problem. If you do (gen-sentence corpus 150), it should generate
;; one very huge sentence, not multiple sentences ending with a terminal
;; character whose word counts add up to 150.
;; TODO: Stack overflow at ~300 depth with loremipsum!
(defn gen-sentence
  [{:keys [end] :as datamap} n]
  {:pre [(pos? n)]}
  (let [coll (if-let [s (dfs-begin datamap n)] s (dfs-single datamap n))]
    (->> coll
         (rest)
         (map last)
         (concat (first coll))
         (reduce #(if (end %2) 
                      (str %1 %2)
                      (str %1 \space %2))))))

(prn "5: " (dfs-begin datamap 5))
(prn "1: " (gen-sentence datamap 1))
(prn "5: " (gen-sentence datamap 5))
;(prn (gen-sentence datamap 75))
(prn (gen-sentence datamap 8)) ; This one has some minor backtracking
; (prn (gen-sentence datamap -1))
; (prn (dive (pick-word (datamap :start)) datamap 300 0))

(defn try-run
  [n]
  (try 
    (doall (gen-sentence datamap n))
    (prn (str "Successfully ran " n "!"))
    (catch StackOverflowError e (prn (str "Stack Overflow! " n)))))

(doall
  (map try-run [275 280]))

;; I suspect what's happening here is it's holding onto the evaluated branches?

(def datamap (read-into-datamap "resources/public/corpora/test/repeating1.json"))
(prn (gen-sentence datamap 5)) ; make sure it's proper
(doall
  (map try-run [275 280]))