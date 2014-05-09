(ns wysitutwyg.markov.textgen
  (:require [wysitutwyg.markov.builder :as builder]
            [clojure.data.json :as json]))

;;; TODO: caching! Except that this isn't, you know, necessarily the best path.
;;; It's in-memory caching which is...Not Ideal, shall we say?
(def cache (atom {}))

;;; TODO: What if you want to break on BEGIN and END blocks?
(def block-delimiters #{\. \! \?})
(def word-delimiters #{\space})

(defn parse-into-datamap
  [name string]
  (let [initial (into {}
                  (for [[k v] (json/read-str string)]
                    [(keyword k) v]))]
    (-> initial
        (assoc :name name)
        (update-in [:end] #(into #{} %))
        (update-in [:start] #(apply hash-map (apply concat %)))
        (update-in [:counts] #(apply hash-map (apply concat %))))))

(defn read-into-datamap
  "Slurps and parses. Adds the :name field into the datamap as the file uri."
  [slurpable]
  (parse-into-datamap (str slurpable) (slurp slurpable)))

;;; TODO: Make this less silly.
(defn string-to-integer
  "Primitive 'I do not care about collisions, replace later' hash."
  [s]
  (reduce + (map #(* (int (.charValue %1)) %2) s (range))))
(defn sentence-to-integer
  "This is also a silly function."
  [s]
  (reduce + (map string-to-integer s)))

(defn break-text
  "Breaks a text by word delimiters, discarding word delimiters, and block 
  delimiters, keeping block delimiters as individual nodes."
  [text]
  (->> text
       (partition-by #(not (word-delimiters %)))
       (filter #(not (word-delimiters (first %))))
       (mapcat #(partition-by (comp not block-delimiters) %))))

(defn to-seed-count-maps
  "Breaks text into seed/count maps to be consumed by the builder."
  [text]
  (->> text
       (partition-by (comp block-delimiters first))
       (filter (comp not block-delimiters ffirst))
       (map #(hash-map :seed (sentence-to-integer %)
                       :count (count %)))))

(defn- build-string
  "Builds a single string with specified info."
  [datamap {:keys [count seed]}]
  (builder/with-seed seed (builder/gen-sentence datamap count)))

(defn build-strings
  "Takes a collection of generation requests and resolves to a string."
  [datamap requests]
  (->> requests
       (map #(build-string datamap %))
       (interpose \space) ;; TODO: What if you want to use something else?
       (apply str)))

(defn transform
  "Takes a datamap and string and runs the transformation."
  [datamap string]
  (->> (break-text string)
       (to-seed-count-maps)
       (build-strings datamap)))

#_(def datamap (read-into-datamap "resources/public/corpora/loremipsum/1.json"))
#_(prn (build-strings datamap (to-seed-count-maps (break-text test-text))))