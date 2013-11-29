(ns formatter.extensions.comment-length
  (:use formatter.extension))
  
(defn process-comment [[k v]]
  (do (prn "COMMENT FOUND!")
      [k v]))
  
(defn find-comment [tree]
  (cond
    (and (vector? tree) (= (first tree) :Comment)) (process-comment tree)
    (vector? tree) (do (prn tree) (map find-comment tree))
    :else tree))
          
(reify FormatterExtension
  (is-active [this] true)
  (modify-tree [this tree] (find-comment tree)))