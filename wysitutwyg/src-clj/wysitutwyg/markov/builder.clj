(ns wysitutwyg.markov.builder
  (:require [clojure.data.json :as json]))

(def timeout-msec 10000)
(defn get-now
  "Get the current time in milliseconds, using Date.getTime()."
  []
  (. (java.util.Date.) getTime))
  
;; Fixes seed for reproducability
(def ^:dynamic ^java.util.Random *rnd* (java.util.Random. 1))
(defn- get-rand [x]
  (.nextInt *rnd* x))

(defmacro with-seed
  [seed & code]
  `(binding [*rnd* (java.util.Random. ~seed)]
    ~@code))

(defn word-count
  "Returns the word count of a string."
  [s]
  (count (clojure.string/split s #"\s")))

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

(defn gen-next-frame
  "Builds the next frame, applying the func parameter to the depth to produce
  the next depth."
  [inc-depth? 
   {:keys [counts end] :as datamap} 
   {:keys [depth node edges] :as frame}]
  (let [next-node (conj (vec (rest node)) (first edges))]
    {:depth (if inc-depth? (inc depth) depth)
     :node next-node
     :edges (pick-words (counts next-node))}))

;;; This is just a huge cascading mess
;;; TODO: Make it less silly
(defn- dfs
  "Runs a dfs to produce a sequence of tokens of n depth ending on a end node."
  [{:keys [counts end] :as datamap} n stack]
  (let [end-time (+ (get-now) timeout-msec)]
    (loop [stack stack]
      (if-let [{:keys [depth node edges] :as node-info} (first stack)]
        (cond
          (> (get-now) end-time)
            nil
          (and (= (+ depth (dec (count node))) n) ; TODO: lol
               (end (last node)))
            (reverse (map #(% :node) stack))
          (or (end (last node)) 
              (= depth n) 
              (empty? edges))
            (recur (rest stack))
          :else
            (let [next-info (gen-next-frame true datamap node-info)
                  new-node-info (update-in node-info [:edges] rest)
                  next-stack (conj (cons new-node-info (rest stack)) next-info)]
              (recur next-stack)))))))

(defn- dive
  "Runs a dfs, with no punctuation constraints."
  [{:keys [counts end] :as datamap} n stack]
  (if-let [{:keys [depth node edges] :as node-info} (first stack)]
    (cond
      (>= (+ depth (count node)) n) ; TODO: lol
        (reverse (map #(% :node) stack))
      (empty? edges)
        (recur datamap n (rest stack))
      :else
        (let [next-info (gen-next-frame (not (end (first edges))) 
                                        datamap 
                                        node-info)
              new-node-info (update-in node-info [:edges] rest)
              next-stack (conj (cons new-node-info (rest stack)) next-info)]
          (recur datamap n next-stack)))))

(defn- run-func
  [f {:keys [start counts] :as datamap} n]
  {:pre [(pos? n)]}
  (->> (pick-words start)
       (map #(f datamap n [{:depth 0
                            :node %
                            :edges (pick-words (counts %))}]))
       (remove nil?)
       (first)))

(defn gen-sentence
  [{:keys [end] :as datamap} n]
  {:pre [(pos? n)]}
  (let [coll (if-let [s (run-func dfs datamap n)]
                     s
                     (run-func dive datamap n))]
    (->> coll
         (rest)
         (map last)
         (concat (first coll))
         (reduce #(if (end %2) 
                      (str %1 %2)
                      (str %1 \space %2))))))

#_(do 
  (def map-loc "resources/public/corpora/loremipsum/2.json")
  (use 'wysitutwyg.markov.textgen)
  (def dmap (read-into-datamap map-loc))
  (prn (str "30: " (gen-sentence dmap 30)))

  ; confirm timeout operational
  (def timeout-msec 1)
  (with-seed 1 (time (doall (map #(gen-sentence dmap %) (range 1 100)))))
  (def timeout-msec 1000)
  (with-seed 1 (time (doall (map #(gen-sentence dmap %) (range 1 100))))))

; -----=====##### CONCERNING FALLBACK TEXT GENERATION BEHAVIOR #####=====-----
;   OKAY I get why some generated texts are slightly shorter than n! That's 
; because punctuation counts as a word, so if the dive hits the end of a 
; sentence...bam! One extra level down.
;   Question: should the fallback text be able to generate sentences, or should 
; it, well, not? It'd look super funny if it didn't but then the normal text 
; looks super funny at high lengths, so...? But, no, it should not have any 
; punctuation at all!
;   Okay, hold on. Let's assume a grammar of a, b, c, d...z, where every node 
; leads to only the next node in the alphabet, and a "." which is our end node.
;   What happens when you try to generate a fifty-word sentence? The dfs will 
; return nil, because there is no valid fifty-word sentence. If we try to do 
; a fifty-word dive, without punctuation, we have three options:
;   * Return a combination of sentences which collectively have fifty words
;   * Return the truncated twenty-six word sentence
;   * Return nil, and you need ANOTHER fallback algorithm! Actually, that'd be 
; a little like trying to get blood out of a stone; there's really no way to 
; generate a fifty-word sentence from a linear sequence of twenty-six words.
;   So, if this is the case, we MUST accept punctuation in our fallback 
; sentences if we want to get a text with fifty words, OR accept that the 
; text will be less than fifty words.
;   I will do the former.

;;; -----=====##### CONCERNING 153 #####=====-----
;   Watching what's happening using the true-dfs code is quite interesting. One
; thing that could be improved is predicting end branches. That is, if a node
; will inevitably lead to a end node or terminating node, mark it as such with
; the length to the end, so that it doesn't actually have to go down and
; attempt to resolve it. That would speed it up in some situations where the
; chain is very loosely interconnected (graph is relatively sparse).
;   Also, obviously, caching would be a really good idea.
;   In a "production" environment, though, even if it does cache, taking half
; an hour to resolve the first time generation of a full-tree traversal is very
; much not ideal. Can we return partial or incremental results?
;   Unfortunately...not really, no, at least not when we're attempting to
; compute a sentence ending in a specific node. We can return *temporary*
; results, but not *partial* results (temporary by, for example, switching to
; the "doesn't care about end nodes" algorithm while also leaving the main
; computation running in the background).
;   Okay, so it's the day after aaaand...I think I'm somehow allowing it to 
; loop infinitely. The nature of the depth comparison should make that not
; possible, in the traditional looping sense (as in, going around in circles
; attempting to satisfy the end criteria - because it will be assured to end
; when depth is overrun) but this is taking Basically Forever.
;   Well, the algorithm is exponential, and we want to go for 153 deep, 
; so...uh, yeah, that's Basically Forever if no such sentence exists and it has
; to try and enumerate every node. Horrifyingly slow. As far as I can remember
; there isn't really any way to remedy this - the efficiency is exponential by
; size to branching factor. So, actually, you wouldn't need a loop to take
; forever - to depth 153 at 3 branching, that's 3^153=Infinity Nodes To Search.
; I mean, not technically infinity, but yeah, that's not happening.
;   Hmm. That's troublesome, though.
;   What would be a good way to estimate how long it *should* take? Because
; right now I'm thinking maybe it would be best to estimate how long it should
; take, and then if it takes longer than that by some margin, go and hit the
; fallback algorithm.