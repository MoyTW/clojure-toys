(ns wysitutwyg.markov
  (:require [clojure.data.json :as json]))

(def delimiters #{\space})

(defn not-delimiter? [char]
  (not (delimiters char)))
 
(defn parse-counts
  "Transforms the corpus into a mapping of n-grams to counts, of the form:
  [[['He' 'is'] {'a' 2}], [['is' 'a'] {'sad' 2, 'mad' 2}]]. The output is a
  vector of key/value vectors for easy json serialization, which may be easily
  converted back through #(into {} %)."
  [n corpus-string]
  (loop [counts {}
         corpus (filter (comp not-delimiter? first)
                        (partition-by not-delimiter? corpus-string))]
    (let [[state rest-corpus] (split-at n corpus)
          follows (first rest-corpus)]
      (if (seq rest-corpus)
          (recur (update-in counts [state follows] (fnil inc 0)) (rest corpus))
          counts))))

(defn keys-to-strings [s]
  (vec (map #(apply str %) s)))

(defn inner-map [m]
  (into {} (map (fn [[k v]] [(apply str k) v]) m)))

(defn stringify
  [m]
  (->> m 
       (map #(update-in % [0] keys-to-strings))
       (map #(update-in % [1] inner-map))))

(defn parse-and-save
  [infile outfile n]
  (with-open [w (clojure.java.io/writer outfile)]
    (spit outfile (json/write-str (stringify (parse-counts n (slurp infile)))))))
 
(parse-and-save 
  "resources/public/corpora/sonnets/corpus.txt" 
  "resources/public/corpora/sonnets/onegram.json" 
  1)
(parse-and-save 
  "resources/public/corpora/sonnets/corpus.txt" 
  "resources/public/corpora/sonnets/twogram.json" 
  2)