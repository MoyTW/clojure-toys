(ns formatter.extensions.comment-length-test
  (:use formatter.extension)
  (:require [clojure.test :refer :all]
            [formatter.parser :as par]))

(def robj (load-file "src/formatter/extensions/comment_length.clj"))
            
(def t0-in
";;   This is a line of exceptional length! It should be broken into two lines! The whitespace at the end of the lines may be intact.")
(def t0-out
";;   This is a line of exceptional length! It should be broken into two lines! 
;; The whitespace at the end of the lines may be intact.")
(def map-0 {:in (par/parser t0-in :unhide :content) 
            :out (par/parser t0-out :unhide :content)})
(deftest test-breaks
  (testing "Tests that it will change the tree if it should"
    (is (= (modify-tree robj (:in map-0))
           (:out map-0)))))

(def t1-str
"(fn custom-interpose [sep, in-seq]
  (butlast (flatten
   (map (fn [in, sep] [in sep]) in-seq (take (count in-seq) (repeat sep))))))")
(def map-1 {:in (par/parser t1-str :unhide :content) 
            :out (par/parser t1-str :unhide :content)})
(deftest test-does-not-break
  (testing "Tests that unmodified does not change tree"
    (is (= (modify-tree robj (:in map-1))
           (:out map-1)))))