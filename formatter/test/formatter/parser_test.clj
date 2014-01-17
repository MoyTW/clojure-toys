(ns formatter.parser-test
  (:require [clojure.test :refer :all]
            [formatter.parser :as par]
            [instaparse.core :as insta]))

(def t1-str
"(when pred
    (foo)
    (bar))

(when (not true)
  :f
  :t)")
(deftest test-reconstruct-one
  (testing "Tests that it can reconstruct the statement"
    (is (= (par/htree-to-str (par/parser t1-str :unhide :content))
           t1-str))))
           
(def t2-str
"(when pred
    (foo)
    (bar))

(when-not true
  :f
  :t)")
(deftest test-reconstruct-two
  (testing "Tests that it can reconstruct the statement"
    (is (= (par/htree-to-str (par/parser t2-str :unhide :content))
           t2-str))))

(def t3-str
"(when pred ;this is a comment!
    (foo)
    (bar)) ; also a comment
;;; - three-char comment, this should not throw an error
;; blah")
;(prn (par/parser t3-str :unhide :content :total true))
(deftest test-reconstruct-comments
  (testing "Tests that it can reconstruct the statement"
    (is (= (par/htree-to-str (par/parser t3-str :unhide :content))
           t3-str))))
           
(def t4-str
"; This is one very long comment which is more than the eighty characters which we intend to break comments over. What does it look like when translated to a tree, I wonder? Let's find out!")
;(prn (par/parser t4-str :unhide :content :total true))
(deftest test-long-comment
  (testing "Tests that it can reconstruct the statement"
    (is (= (par/htree-to-str (par/parser t4-str :unhide :content))
           t4-str))))

(def result-74-75 (par/parser (slurp "test/resources/74_75.txt")))
(if (insta/failure? result-74-75) (prn result-74-75))
(deftest test-74-75
  (testing "Tests that it parses my solution to 4Clojure 74-75"
    (is (not (insta/failure? result-74-75)))))