(ns formatter.core
  (:require [formatter.parser :as par]
            [clojure.java.io :as io]
			[formatter.extensions :as exts])
  (:gen-class))
(use '[clojure.tools.cli :only[cli]])

(def extensions exts/all-extensions)

(defn apply-extension [extension tree-and-changes]
  (if (:is-active extension)
      ((:process-code extension) tree-and-changes)
      tree-and-changes))
  
(defn apply-all-extensions [tree]
  (let [result-tree (reduce (fn [t n] (apply-extension n t))
                            {:tree tree :changes [] :suggestions []}
                            extensions)]
    (assoc result-tree :text (par/htree-to-str (:tree result-tree)))))

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
      (let [result (apply-all-extensions [tree []])]
        (if (:out-file p-map)
            (spit (:out-file p-map) result)
            (prn result))))))