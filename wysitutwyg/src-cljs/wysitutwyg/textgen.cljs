(ns wysitutwyg.textgen
  (:require [wysitutwyg.markov :as markov]))

(def text-delimiters #{\. \! \?})
(def word-delimiters #{\space})

(defn not-text-delim? [char]
  (not (text-delimiters char)))
(defn not-word-delim? [char]
  (not (word-delimiters char)))
  
(defn seed-from-sentence ;; TODO: Something better than literally count.
  [sentence]
  (count sentence))

(defn break-text
  "Breaks a text by pred, much like C#'s split."
  [pred text]
  (->> text
       (partition-by pred)
       (filter (comp pred first))))
  
(defn gen-from-sentence
  "Generates a sentence of length equal to the sentence, utilizing a hash or
  other derived value from the given sentence as the randomization seed.
  As you can see from this docstring, it's not quite nailed down yet."
  [sentence chain]
  (binding [markov/*c-rand* (atom (seed-from-sentence sentence))]
    (let [key (nth (vec (keys chain))
                   (markov/get-rand (count (keys chain))))
          num-words (count (break-text not-word-delim? sentence))]
      (markov/generate-text key num-words chain))))

(defn gen-from-text
  "Generates a text from a text block (presumably of sentences)."
  [text chain]
  (->> text
       (break-text not-text-delim?)
       (map #(gen-from-sentence % chain))
       (interpose \space)
       (apply str)))