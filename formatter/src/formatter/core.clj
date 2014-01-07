(ns formatter.core
  (:require [formatter.parser :as par]
            [clojure.java.io :as io])
  (:gen-class))
(use '[clojure.tools.cli :only[cli]])

(def files (rest (file-seq (io/as-file (io/resource "extensions")))))
(def extensions (map #(load-file (.getAbsolutePath %)) files))

(defn apply-extension [extension tree]
  (if (:is-active extension)
      ((:modify-tree extension) tree)
      tree))
  
(defn apply-all-extensions [tree]
  (let [result-tree (reduce (fn [t n] (apply-extension n t)) tree extensions)]
    (par/htree-to-str result-tree)))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (let [[p-map p-trailing p-docstr] 
          (cli args 
               ["-i" "--in-file" "File to run comment processing on."]
               ["-o" "--out-file" "Output file."])
        tree (try 
               (par/parser (slurp (:in-file p-map)) :unhide :content)
               (catch IllegalArgumentException e))]
    (if-not tree (prn p-docstr)
      (let [result (apply-all-extensions tree)]
        (if (:out-file p-map)
            (spit (:out-file p-map) result)
            (prn result))))))