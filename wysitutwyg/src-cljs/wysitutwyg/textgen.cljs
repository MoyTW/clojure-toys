(ns wysitutwyg.textgen
  (:require [wysitutwyg.markov :as markov]))

(def cache (atom {}))
  
(def text-delimiters #{\. \! \?})
(def word-delimiters #{\space})

(defn not-text-delim? [char]
  (not (text-delimiters char)))
(defn not-word-delim? [char]
  (not (word-delimiters char)))

(defn seed-from-sentence
  "Takes a string and maps it to an integer by summing the unicode character
  codes of the characters in the string."
  [sentence]
  (reduce + (map #(.charCodeAt % 0) sentence)))

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

;; TODO: Associate atom /w a corpus (chain)!
(defn text-from-sentence
  "First attempts to find cached sentence in atom cache. If none, generates."
  [sentence chain]
  (if-let [cached (get @cache sentence)]
    cached
    (let [output (gen-from-sentence sentence chain)]
      (do (swap! cache assoc sentence output)
          output))))

(defn gen-from-text
  "Generates a text from a text block (presumably of sentences)."
  [text chain]
  (->> text
       (break-text not-text-delim?)
       (map #(text-from-sentence % chain))
       (interpose \space)
       (apply str)))