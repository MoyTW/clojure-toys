(ns formatter.extensions.comment-length-test
  (:require [clojure.test :refer :all]
            [formatter.parser :as par]
            [clojure.java.io :as io]))

(def fpath (str (io/as-file (io/resource "extensions/comment_length.clj"))))
(def robj (load-file fpath))
(defn do-modification [tree]
  (:tree ((:process-code robj) {:tree tree})))
            
(def t0-in
";;   This is a line of exceptional length! It should be broken into two lines! The whitespace at the end of the lines will be stripped.")
(def t0-out
";;   This is a line of exceptional length! It should be broken into two lines!
;; The whitespace at the end of the lines will be stripped.")
(def map-0 {:in (par/parser t0-in) 
            :out (par/parser t0-out)})
(deftest test-breaks
  (testing "Tests that it will change the tree if it should"
    (is (= (do-modification (:in map-0))
           (:out map-0)))))

(def t1-str
"(fn custom-interpose [sep, in-seq]
  (butlast (flatten
   (map (fn [in, sep] [in sep]) in-seq (take (count in-seq) (repeat sep))))))")
(def map-1 {:in (par/parser t1-str) 
            :out (par/parser t1-str)})
(deftest test-does-not-break
  (testing "Tests that unmodified does not change tree"
    (is (= (do-modification (:in map-1))
           (:out map-1)))))