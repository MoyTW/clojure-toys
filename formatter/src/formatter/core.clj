(ns formatter.core
  (:use formatter.extension)
  (:require [formatter.parser :as par])
  (:gen-class))
(use '[clojure.tools.cli :only[cli]])

(defn apply-extension [file tree]
  (let [ext (load-file (.getAbsolutePath file))]
    (if (is-active ext)
        (modify-tree (load-file (.getAbsolutePath file)) tree)
        tree)))
  
(defn apply-all-extensions [files tree]
  (reduce (fn [t n] (apply-extension n t)) tree files))
               
(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (let [[p-map p-trailing p-docstr] 
          (cli args 
               ["-i" "--in-file" "File to run comment processing on."]
               ["-o" "--out-file" "Output file."])
        tree (try 
               (par/parser (slurp (:in-file p-map)) :unhide :content)
               (catch IllegalArgumentException e))
        files (rest 
                (file-seq 
                  (clojure.java.io/file "src/formatter/extensions")))]
    (if-not tree (prn p-docstr)
      (let [result-tree (apply-all-extensions files tree)
            result-string (par/htree-to-str result-tree)]
        (if (:out-file p-map)
            (spit (:out-file p-map) result-string)
            (prn result-string))))))