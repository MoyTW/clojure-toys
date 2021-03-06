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

(def files-2014-01-24 (map (comp par/parser slurp) (rest (file-seq (clojure.java.io/file "test/resources/2014_01_24")))))
(prn (filter insta/failure? files-2014-01-24))
(deftest test-formatter-files
  (testing "Tests that it parses formatter files as of 2014-01-24"
    (is (every? false? (map insta/failure? files-2014-01-24)))))

(def compojure-snippets (par/parser (slurp "test/resources/compojure_snippets.txt")))
(if (insta/failure? compojure-snippets) (prn compojure-snippets))
(deftest test-compojure-snippets
  (testing "Tests snippets from compojure"
    (is (not (insta/failure? compojure-snippets)))))

(def lein-compile (par/parser (slurp "test/resources/lein_compile_source.txt")))
(if (insta/failure? lein-compile) (prn lein-compile))
(deftest test-lein-compile-source
  (testing "Tests lein-compile."
    (is (not (insta/failure? lein-compile)))))