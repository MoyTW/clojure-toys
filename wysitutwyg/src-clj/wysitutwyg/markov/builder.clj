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
  [state {:keys [counts end] :as datamap} n depth cont]
  (if (>= (+ depth (count state)) n)
      (conj cont state)
      (let [next-word (pick-word (counts state))
            next-depth (if (end next-word) depth (inc depth))
            result (conj (vec (rest state)) next-word)]
        (recur result datamap n next-depth (conj cont state)))))

(defn- dfs-single
  [{:keys [start counts] :as datamap} n]
  {:pre [(pos? n)]}
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

(defn sentence-length
  [s]
  (count (clojure.string/split s #"\s")))

#_(defn test-both [n]
  (do
    (prn "LENGTH: " n)
    (binding [*rnd* (java.util.Random. 1)]
      (let [sentence (gen-sentence dmap n)]
        (prn "Psdo - LEN: " (sentence-length sentence) " STR: " sentence)))
    (binding [*rnd* (java.util.Random. 1)]
      (let [sentence (gen-sentence-true-dfs dmap n)]
        (prn "Psdo - LEN: " (sentence-length sentence) " STR: " sentence)))))
(defn test-both [n]
  (let [old (binding [*rnd* (java.util.Random. 1)]
              (gen-sentence dmap n))
        new (binding [*rnd* (java.util.Random. 1)]
              (gen-sentence-true-dfs dmap n))]
    (do
      (if-not (= n (sentence-length old))
              (prn (str "OLD! N: " n " LEN: " (sentence-length old)
                        " SEN: " old)))
      (if-not (= n (sentence-length new))
              (prn (str "NEW! N: " n " LEN: " (sentence-length new)
                        " SEN: " new))))))

#_(doall (map test-both (range 1 100)))
;   Whoa okay, wow, some of this is super duper broken. Uh, is my end case
; HORRIBLY WRONG?

; -----=====##### CONCERNING NEW ALGORITHM GENNING SHORT #####=====-----
; Hmm, they all appear to be 1 off?
; No, they're not. See: #89, #87.
(doall (map test-both (range 30 50)))
(doall (map test-both (range 85 90)))

; -----=====##### CONCERNING #89 AND #50 #####=====-----
;   Okay, so, for #89 and #50 old, they both end on "resultant pleasure?" - 
; which is the last set of tokens in the corpus! So it has nowhere to go from
; there, since it's using 2-tuples, and so it goes woefully under-sized. That's
; because my dive doesn't do any backtracking. uuuuuuuuh why doesn't it do any
; backtracking? Past-me, I AM VERY CROSS.

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