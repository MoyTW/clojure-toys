(ns wysitutwyg.markov
  (:require [clojure.data.json :as json]))

(def delimiters #{\space})

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

(defn a-s [s] (vec (map #(apply str %) s)))

(defn stringify
  [m]
  (into {} (map #(update-in % [0] a-s) m)))

(defn parse-and-save
  [infile outfile n]
  (with-open [w (clojure.java.io/writer outfile)]
    ;(json/write (stringify (parse-counts n "The quick brown fox jumped")) w)))
    (spit outfile (json/write-str (stringify (parse-counts n (slurp infile)))))))
  
(parse-and-save "F:/aP-Personal_Projects/Clojure/wysitutwyg/resources/public/corpora/sonnets/corpus.txt"
"F:/aP-Personal_Projects/Clojure/wysitutwyg/resources/public/corpora/sonnets/onegram.json" 1)
