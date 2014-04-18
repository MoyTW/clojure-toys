(ns wysitutwyg.markov.parser
  (:require [clojure.data.json :as json]))

(def test-text "Me is mad! It is mad? She is mad. He is sad! It is sad? She is sad. Ze mad? Ze sad? Ze jelly? Ha! He is mad! He is mad!")
  
(def delimiters #{\space})
(def end-nodes #{\. \! \?})
; How to handle ellipses?

(defn not-delimiter? [char]
  (not (delimiters char)))

(defn process-corpus [corpus]
  "Processes the corpus into a collection of segments, determined by the
  delimiter set. Members of end-nodes are grouped into their own segments."
  (->> corpus
       (partition-by not-delimiter?)
       (filter (comp not-delimiter? first))
       (mapcat #(partition-by end-nodes %))))

(defn keys-to-strings [s]
  (vec (map #(apply str %) s)))

(defn inner-map [m]
  (into {} (map (fn [[k v]] [(apply str k) v]) m)))

(defn stringify
  [counts]
  (->> counts
       (map #(update-in % [0] keys-to-strings))
       (map #(update-in % [1] inner-map))))

(defn update-start
  "Updates iff the arity of the key is equal to n."
  [start n follows rest-corpus]
  ;TODO: ugly
  (if (= n 1)
      (update-in start [[follows]] (fnil inc 0))
      (let [key [(apply conj [follows] (take (dec n) (rest rest-corpus)))]] 
        (if (= n (apply count key))
            (update-in start key (fnil inc 0))
            start))))

(defn build-output-map
  [start counts]
  {:start (map #(update-in % [0] keys-to-strings) start)
   :end (map str end-nodes)
   :counts (stringify counts)})

(defn parse-step
  "Recursive step producing a map of the form 
  {:start {STATE0 COUNT0, STATE1 COUNT1, ... STATEn COUNTn}
   :end (ENDSTR0, ENDSTR1 ... ENDSTRn)
   :counts {STATE0 NEXT0, STATE1 NEXT1, ... STATEn NEXTn}}
  where STATEn is an array of strings
  COUNTn is an int
  NEXTn := [STATEn {STR0 COUNT0, STRn, COUNTn}]
  This isn't really good documentation; it'll confuse people who aren't me."
  [n counts start corpus]
  (let [[state rest-corpus] (split-at n corpus)
         follows (first rest-corpus)]
    (cond
      (and (seq rest-corpus) (end-nodes (first (last state))))
        (recur n
               (update-in counts [state follows] (fnil inc 0))
               (update-start start n follows rest-corpus)
               (rest corpus))
      (seq rest-corpus)
        (recur n
               (update-in counts [state follows] (fnil inc 0))
               start
               (rest corpus))
      :else
        (build-output-map start counts))))

;; Does not properly generate :start nodes for counts > 1!
;; Actually, it doesn't really know how to handle counts > 1 at all...hmm.
;; It's a bit of a monster function, too. Unwieldy, at best.
;; TODO: Refactor this?
;; TODO: Hey, it's missing the first one! That is - the very first. Hmm.
(defn parse-counts
  "Creates a mapping of states to counts of the form 
  {start-state {end-state n, end-state n}}
    start-state | ((\\w \\o \\r \\d \\1) (\\w \\2))
    end-state | (\\w \\o \\r \\d)
  with the start and end states mapped to :start and :end."
  [n corpus-string]
  (let [processed (process-corpus corpus-string)]
    (parse-step n
                {}
                (update-start {} n (first processed) processed)
                processed)))

(defn parse-and-save
  [infile outfile n]
  (with-open [w (clojure.java.io/writer outfile)]
    (spit outfile (json/write-str (parse-counts n (slurp infile))))))
    
(defn do-parses []
  (do
    (parse-and-save
      "resources/public/corpora/test/test.txt"
      "resources/public/corpora/test/test1.json"
      1)
    (parse-and-save
      "resources/public/corpora/test/repeating.txt"
      "resources/public/corpora/test/repeating1.json"
      1)
    (parse-and-save 
      "resources/public/corpora/sonnets/corpus.txt" 
      "resources/public/corpora/sonnets/1.json" 
      1)
    (parse-and-save 
      "resources/public/corpora/sonnets/corpus.txt" 
      "resources/public/corpora/sonnets/2.json" 
      2)
    (parse-and-save 
      "resources/public/corpora/sonnets/corpus.txt" 
      "resources/public/corpora/sonnets/3.json" 
      3)
    (parse-and-save 
      "resources/public/corpora/loremipsum/corpus.txt" 
      "resources/public/corpora/loremipsum/1.json" 
      1)
    (parse-and-save 
      "resources/public/corpora/loremipsum/corpus.txt" 
      "resources/public/corpora/loremipsum/2.json" 
      2)
    (parse-and-save 
      "resources/public/corpora/loremipsum/corpus.txt" 
      "resources/public/corpora/loremipsum/3.json" 
      3)))
(do-parses)