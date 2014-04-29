(ns wysitutwyg.markov.builder
  (:require [clojure.data.json :as json]))

;; Fixes seed for reproducability
(def ^:dynamic ^java.util.Random *rnd* (java.util.Random. 1))
(defn- get-rand [x]
  (.nextInt *rnd* x))

(defmacro with-seed
  [seed & code]
  `(binding [*rnd* (java.util.Random. ~seed)]
    ~@code))

(defn- pick-word
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

(defn- pick-words
  "Returns a lazy sequence of the next states from the start state, generated
  by repeatedly picking words."
  [counts]
  (letfn [(step [candidates]
            (if-let [w (pick-word candidates)]
              (cons w (step (dissoc candidates w)))
              nil))]
    (lazy-seq (step counts))))

(defn- find-first-end
  "Returns the first end node following the specified state, nil if none."
  [state {:keys [counts end] :as datamap}]
  (first (filter end (pick-words (counts state)))))

(defn- words-to-states
  "Takes a state and a collection of words, and maps it to the next set of
  states."
  [state words]
  (map #(conj (vec (rest state)) %) words))

(defn- gen-next-continuations
  [{:keys [counts end] :as datamap} {:keys [depth queue state]}]
  (->> (counts state)
       (pick-words)
       (words-to-states state)
       (remove #(end (last %)))
       (map #(hash-map :depth (inc depth)
                       :queue (conj queue state)
                       :state %1))))

;; cont consists of: {:depth depth, :queue string, :state state}
;;   Works for 2-3 tuples with caveats - many fewer viable paths, so drops to
;; dive a lot more! Also - it has n too many words, where n is one less than
;; the size of the tuple.
(defn- dfs-cont
  [{:keys [counts end] :as datamap} n continuations]
  (if-let [{:keys [depth queue state] :as cont} (first continuations)]
    (if (>= (+ depth (count state)) n)
        (if-let [end-state (find-first-end state datamap)]
          (conj queue state (vector end-state))
          (recur datamap n (rest continuations)))
        (if-let [new-conts (seq (gen-next-continuations datamap cont))]
          (recur datamap n (apply conj (rest continuations) new-conts))
          (recur datamap n (rest continuations))))))

(defn gen-next-info
  [{:keys [counts end] :as datamap} {:keys [depth node edges]}]
  (let [next-node (conj (vec (rest node)) (first edges))]
    {:depth (inc depth)
     :node next-node
     :edges (->> (counts next-node)
                 (pick-words))}))

;; More true implementation of dfs
(defn dfs-true
  [{:keys [counts end] :as datamap} n stack]
  (if-let [{:keys [depth node edges] :as node-info} (first stack)]
    (cond
      (and (= (+ depth (dec (count node))) n) ; TODO: lol
           (end (last node)))
        (reverse (map #(% :node) stack))
      (or (= depth n) (empty? edges)) (recur datamap n (rest stack))
      :else
        (let [next-info (gen-next-info datamap node-info)
              new-node-info (update-in node-info [:edges] rest)]
          (recur datamap n (conj (cons new-node-info (rest stack)) next-info))))))

(defn- dfs-true-begin
  [{:keys [start counts] :as datamap} n]
  {:pre [(pos? n)]}
  (->> (pick-words start)
       (map #(dfs-true datamap n [{:depth 0 :node % :edges (pick-words (counts %))}]))
       (remove nil?)
       (first)))

(defn- dfs-begin
  [{:keys [start counts] :as datamap} n]
  {:pre [(pos? n)]}
  (->> (pick-words start)
       (map #(dfs-cont datamap n [{:queue [] :state % :depth 0}]))
       (remove nil?)
       (first)))

;; Tail-recursive
(defn- dive
  [state {:keys [counts] :as datamap} n depth cont]
  (if (>= (+ depth (count state)) n)
      (conj cont state)
      (let [result (conj (vec (rest state)) (pick-word (counts state)))]
        (recur result datamap n (inc depth) (conj cont state)))))

(defn- dfs-single
  [{:keys [start counts] :as datamap} n]
  {:pre [(pos? n)]}
  (prn "Diving!")
  (dive (pick-word start) datamap n 0 []))

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

(defn gen-sentence-true-dfs
  [{:keys [end] :as datamap} n]
  {:pre [(pos? n)]}
  (let [coll (if-let [s (dfs-true-begin datamap n)] s (dfs-single datamap n))]
    (->> coll
         (rest)
         (map last)
         (concat (first coll))
         (reduce #(if (end %2) 
                      (str %1 %2)
                      (str %1 \space %2))))))

(def map-loc "resources/public/corpora/loremipsum/2.json")
(use 'wysitutwyg.markov.textgen)
(def dmap (read-into-datamap map-loc))

;; TODO: Okay, it doesn't generate the currect length if you have 2-tuples.
;; I thought I fixed that, but I didn't.
;; This is why tests are good.

(defn test-both [n]
  (do
    (prn "LENGTH: " n)
    (binding [*rnd* (java.util.Random. 1)]
      (prn "Psdo: " (gen-sentence dmap n)))
    (binding [*rnd* (java.util.Random. 1)]
      (prn "True: " (gen-sentence-true-dfs dmap n)))))

; (test-both 2)
(test-both 10) ; lol that tricked me into thinking it was length 9!
;;; TODO: Issue: punctuation has its own depth in dives
(test-both 20)
(test-both 40) ;; Issue: 39 not 40!
(test-both 30) ;; Issue: 39 not 40!

#_(binding [*rnd* (java.util.Random. 1)]
  (prn (gen-sentence dmap 500)))

#_(binding [*rnd* (java.util.Random. 1)]
  (prn (gen-sentence-true-dfs dmap 500)))