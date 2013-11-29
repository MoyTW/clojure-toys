(ns formatter.extensions.when-not
  (:use formatter.extension)
  (:require [instaparse.core :as insta]))

;;;; Replace (when (not p) %1 %2) with (when-not %1 %2)
  
;; My gosh, but this is ugly-looking!
(defn replace-when-not [& args]
  (let [[pre-strs [[_ first-symbol] second-vec & more]] (split-with string? args)
       [[_ second-symbol] pred] (filter vector? second-vec)
       pred-ws (if-not (some #(and (string? %) (re-find #"\s" %)) pred)
                       [" "])]
    (if (and (= first-symbol "when") (= second-symbol "not"))
        (->> more
            (concat pre-strs [[:Symbol "when-not"]] pred-ws [pred])
            (cons :Eval)
            (into []))
        (into [] (cons :Eval args)))))
    
(defn fe-modify-tree [tree]
  (insta/transform {:Eval replace-when-not} tree))

(reify FormatterExtension
  (is-active [this] true)
  (modify-tree [this tree] (fe-modify-tree tree)))