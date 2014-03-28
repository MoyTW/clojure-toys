(ns wysitutwyg.markov
  (:require [clojure.data.json :as json]))

(def test-text "He is mad! It is mad? She is mad. He is sad! It is sad? She is sad.")
  
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

;; Does not properly generate :start nodes for counts > 1!
(defn parse-counts
  "Creates a mapping of states to counts of the form 
  {start-state {end-state n, end-state n}}
    start-state | ((\\w \\o \\r \\d \\1) (\\w \\2))
    end-state | (\\w \\o \\r \\d)
  with the start and end states mapped to :start and :end."
  [n corpus-string]
  (loop [counts (apply hash-map (mapcat vector
                                        (map (comp list list) end-nodes)
                                        (repeat :end)))
         corpus (process-corpus corpus-string)]
    (let [[state rest-corpus] (split-at n corpus)
          follows (first rest-corpus)]
      (if (seq rest-corpus)
          (cond 
            (end-nodes (first (last state)))
              (recur (update-in counts [:start follows] (fnil inc 0))
                     (rest corpus))
            :else
              (recur (update-in counts [state follows] (fnil inc 0))
                     (rest corpus)))
          counts))))

;;; Everything below this is broken by the inclusion of keywords.
;;; Until I can figure out how I want to handle them in json, it will remain
;;; so.
;;; Default behavior uses name, and that is just unacceptable, but how should
;;; it be handled such that, if I happened to use the Clojure documentation as
;;; a corpus, it wouldn't break due to the keywords being valid?

#_(defn keys-to-strings [s]
  (vec (map #(apply str %) s)))

#_(defn inner-map [m]
  (into {} (map (fn [[k v]] [(apply str k) v]) m)))

#_(defn stringify
  [m]
  (->> m 
       (map #(update-in % [0] keys-to-strings))
       (map #(update-in % [1] inner-map))))

#_(defn parse-and-save
  [infile outfile n]
  (with-open [w (clojure.java.io/writer outfile)]
    (spit outfile (json/write-str (stringify (parse-counts n (slurp infile)))))))

#_(defn do-parses []
  (doall
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