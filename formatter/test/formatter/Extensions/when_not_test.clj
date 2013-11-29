(ns formatter.extensions.when-not-test
  (:use formatter.extension)
  (:require [clojure.test :refer :all]
            [formatter.parser :as par]))

(def robj (load-file "src/formatter/extensions/when_not.clj"))
            
(def t0-in
"(when (not pred )
       foo
       bar)")
(def t0-out
"(when-not pred
       foo
       bar)")
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
"(when (not true)
  :f
  :t)")
(def t2-out
"(when-not true
  :f
  :t)")
(def map-2 {:in (par/parser t2-in :unhide :content) 
            :out (par/parser t2-out :unhide :content)})
(deftest test-does-modify-pred-not-symbol
  (testing "Tests that it still modifies it, even if the pred is not a symbol"
    (is (= (modify-tree robj (:in map-2))
           (:out map-2)))))
           
(def t3-in
"
(when
      (not
 true)
  :f
  :t)

  ")
(def t3-out
"
(when-not
 true
  :f
  :t)

  ")
(def map-3 {:in (par/parser t3-in :unhide :content) 
            :out (par/parser t3-out :unhide :content)})
(deftest test-whitespace
  (testing "Tests that it handles whitespace properly"
    (is (= (par/htree-to-str (modify-tree robj (:in map-3)))
           (par/htree-to-str (:out map-3))))))

(def t4-in
"
(when
  (not 
    true     )
  :f
  :t)

  ")
(def t4-out
"
(when-not 
    true
  :f
  :t)

  ")
(def map-4 {:in (par/parser t4-in :unhide :content) 
            :out (par/parser t4-out :unhide :content)})
(deftest test-more-whitespace
  (testing "Tests that it handles whitespace properly"
    (is (= (par/htree-to-str (modify-tree robj (:in map-4)))
           (par/htree-to-str (:out map-4))))))
           
(def t5-in
"(when pred
    (foo)
    (bar))

(when (not true)
  :f
  :t)")
(def t5-out
"(when pred
    (foo)
    (bar))

(when-not true
  :f
  :t)")
(def map-5 {:in (par/parser t5-in :unhide :content) 
            :out (par/parser t5-out :unhide :content)})
(deftest test-after-other-statement
  (testing "Tests that it handles multi-statement code snippets"
    (is (= (par/htree-to-str (modify-tree robj (:in map-5)))
           (par/htree-to-str (:out map-5))))))