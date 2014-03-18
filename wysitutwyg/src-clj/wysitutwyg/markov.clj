;; Fixes seed for reproducability
(def ^:dynamic ^java.util.Random *rnd* (java.util.Random. 1))
(defn get-rand [x]
  (.nextInt *rnd* x))

;; Delimiters set
(def delimiters #{\space \newline})

(defn not-delimiter? [char]
  (not (delimiters char)))
 
(defn parse-counts
  "Transforms the corpus into a mapping of n-grams to counts, of the form:
  {['He' 'is'] {'a' 2}, ['is' 'a'] {'sad' 2, 'mad' 2}}"
  [n corpus-string]
  (loop [counts {}
         corpus (filter (comp not-delimiter? first)
                        (partition-by not-delimiter? corpus-string))]
    (let [[state rest-corpus] (split-at n corpus)
          follows (first rest-corpus)]
      (if (seq rest-corpus)
          (recur (update-in counts [state follows] (fnil inc 0)) (rest corpus))
          counts))))
 
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

(defn lazy-chain
  "Given a starting state, returns a lazy sequence of generated words until it
  reaches an end state."
  [start-state counts]
  (if-let [next-word (pick-word (counts start-state))]
    (cons (apply str next-word)
          (lazy-seq (lazy-chain (conj (vec (rest start-state))
                                      next-word)
                                counts)))))

(defn generate-text
  "Given a starting state as a string, generates n further words from the given
  count graph. Ends once n words have been generated, or when a terminal node
  has been reached."
  [state n counts]
  (->> (reverse state)
       (apply conj (->> counts
                        (lazy-chain (map seq state))
                        (take n)))
       (clojure.string/join \space)))