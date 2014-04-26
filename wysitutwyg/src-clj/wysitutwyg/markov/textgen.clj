(ns wysitutwyg.markov.textgen
  (:require [wysitutwyg.markov.builder :as builder]
            [clojure.data.json :as json]))

(defn parse-into-datamap
  [string]
  (let [initial (into {}
                  (for [[k v] (json/read-str string)]
                    [(keyword k) v]))]
    (-> initial
        (update-in [:end] #(into #{} %))
        (update-in [:start] #(apply hash-map (apply concat %)))
        (update-in [:counts] #(apply hash-map (apply concat %))))))

(defn read-into-datamap
  [slurpable]
  (parse-into-datamap (slurp slurpable)))