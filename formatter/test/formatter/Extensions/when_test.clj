(ns formatter.extensions.when-test
  (:use formatter.extension)
  (:require [clojure.test :refer :all]
            [formatter.parser :as par]))

(def robj (load-file "src/formatter/extensions/when.clj"))
            
(def t0-in
"(if pred
  (do
    (foo)
    (bar))
       )")
(def t0-out
"(when pred
    (foo)
    (bar))")
(def map-0 {:in (par/parser t0-in :unhide :content) 
            :out (par/parser t0-out :unhide :content)})
(deftest test-modifies
  (testing "Tests that it will change the tree if it should"
    (is (= (modify-tree robj (:in map-0))
           (:out map-0)))))

(def t1-str
"(fn custom-interpose [sep, in-seq]
  (butlast (flatten
   (map (fn [in, sep] [in sep]) in-seq (take (count in-seq) (repeat sep))))))")
(def map-1 {:in (par/parser t1-str :unhide :content) 
            :out (par/parser t1-str :unhide :content)})

(deftest test-does-not-modify
  (testing "Tests that unmodified does not change tree"
    (is (= (modify-tree robj (:in map-1))
           (:out map-1)))))
           
(def t2-in
"


(if pred
  (do
    (foo)
    (bar))
       )")
(def t2-out
"


(when pred
    (foo)
    (bar))")
(def map-2 {:in (par/parser t2-in :unhide :content) 
            :out (par/parser t2-out :unhide :content)})
(deftest test-pre-whitespace
  (testing "Tests that it maintains whitespace before the statement"
    (is (= (par/htree-to-str (modify-tree robj (:in map-2)))
           (par/htree-to-str (:out map-2))))))
           
(def t3-in
"(if (= 3
        :a
        19)
  (do
 (foo)
   (bar))
       )")
(def t3-out
"(when (= 3
        :a
        19)
 (foo)
   (bar))")
(def map-3 {:in (par/parser t3-in :unhide :content) 
            :out (par/parser t3-out :unhide :content)})
(deftest test-more-spacing
  (testing "Tests that it keeps parameters on different lines (doesn't indent)"
    (is (= (par/htree-to-str (modify-tree robj (:in map-3)))
           (par/htree-to-str (:out map-3))))))