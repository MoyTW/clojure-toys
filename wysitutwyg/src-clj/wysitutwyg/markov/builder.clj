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

(defn pick-words
  "Returns a lazy sequence of the next states from the start state, generated
  by repeatedly picking words."
  [counts]
  (letfn [(step [candidates]
            (if-let [w (pick-word candidates)]
              (cons w (step (dissoc candidates w)))
              nil))]
    (lazy-seq (step counts))))

(defn find-first-end
  "Returns the first end node following the specified state, nil if none."
  [state {:keys [counts end] :as datamap}]
  (first (filter end (pick-words (counts state)))))

(defn words-to-states
  "Takes a state and a collection of words, and maps it to the next set of
  states."
  [state words]
  (map #(conj (vec (rest state)) %) words))

;; cont consists of: {:depth depth, :sequence string, :state state}
;; This is a pretty monstrous function!
;; Does not blow stack.
;; Does not work for 2-tuples 3-tuples etc
(defn dfs-cont
  [{:keys [counts end] :as datamap} n continuations]
  (if (seq continuations)
      (let [{:keys [depth sequence state]} (first continuations)]
        (if (= depth (dec n))
            (if-let [end-state (find-first-end state datamap)]
              (conj sequence state (vector end-state))
              (recur datamap n (rest continuations)))
            (let [next-states (->> (counts state)
                                   (pick-words)
                                   (words-to-states state)
                                   (remove #(end (last %))))
                  new-conts
                    (map #(hash-map :depth (inc depth)
                                    :sequence (conj sequence state)
                                    :state %1)
                         next-states)]
              (if (seq new-conts)
                 (recur datamap n (apply conj (rest continuations) new-conts))
                 (recur datamap n (rest continuations))))))))

(defn dfs-begin
  [{:keys [start counts] :as datamap} n]
  {:pre [(pos? n)]}
  (->> (pick-words start)
       (map #(dfs-cont datamap n [{:sequence [] :state % :depth 0}]))
       (remove nil?)
       (first)))

;; Tail-recursive
(defn dive
  [state {:keys [counts] :as datamap} n depth cont]
  (if (= depth (dec n))
      (conj cont state)
      (let [result [(pick-word (counts state))]]
        (recur result datamap n (inc depth) (conj cont state)))))

(defn dfs-single
  [{:keys [start counts] :as datamap} n]
  {:pre [(pos? n)]}
  (dive (pick-word start) datamap n 0 []))
  
;; TODO: Assumes single tuples.
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

(prn "5: dfs-begin " (dfs-begin datamap 5))
(prn "1: " (gen-sentence datamap 1))
(prn "5: " (gen-sentence datamap 5))
(prn (gen-sentence datamap 8)) ; This one has some minor backtracking
(prn (gen-sentence datamap 9001))