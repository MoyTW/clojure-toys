(ns formatter.extensions.thread-first-test
  (:require [clojure.test :refer :all]
            [formatter.parser :as par]
			[formatter.extensions.thread-first :as ext]))

(defn get-suggestions [code]
  (map :code
       (:suggestions 
         ((:process-code ext/extension) 
           {:tree (par/parser code) 
           :suggestions []}))))

(def from-clojuredocs "(first (.split (.replace (.toUpperCase \"a b c d\") \"A\" \"X\") \" \"))")
(deftest clojuredocs-example
  (testing "Tests (first (.split (.replace (.toUpperCase \"a b c d\") \"A\" \"X\") \" \"))"
    (is (contains? (into #{} (get-suggestions from-clojuredocs)) 
                   from-clojuredocs))))

(def f-to-c "(/ (* (- n 32) 5) 9)")
(deftest test-farenheit-conversion
  (testing "Tests that (/ (* (- n 32) 5) 9) is suggested."
    (is (contains? (into #{} (get-suggestions f-to-c)) f-to-c))))

;; This can be threaded as follows:
;(+ (-> (+ 1 2)
;       (+ 3)
;       (+ 4))
;   (-> (- 5 6)
;       (- 7)
;       (- 8)))
;; We want it to suggest (+ (+ (+ 1 2) 3) 4) and (- (- (- 5 6) 7) 8) only.
(def multiple-paths "(+ (+ (+ (+ 1 2) 3) 4) (- (- (- 5 6) 7) 8))")
(def multiple-suggestions (into #{} (get-suggestions multiple-paths)))

(deftest test-does-not-suggest-branching
  (testing "Tests that if there are two paths, don't suggest top-level form."
    (is (not (contains? multiple-suggestions multiple-paths)))))

(deftest test-suggests-nested-trees
  (testing "Tests that it accepts nested trees."
    (is (and (contains? multiple-suggestions "(+ (+ (+ 1 2) 3) 4)")
             (contains? multiple-suggestions "(- (- (- 5 6) 7) 8)")))))

(def deeply-nested "(+ (+ (+ (+ (+ 1 2) 3) 4) 5) 6)")
(def deeply-suggestions (into #{} (get-suggestions deeply-nested)))

(deftest test-does-not-suggest-strict-subtrees
  (testing "Tests that it doesn't suggest non-highest-level trees."
    (is (not (contains? deeply-suggestions
                        "(+ (+ (+ (+ 1 2) 3) 4) 5)")))))

(deftest suggests-deep-top-level
  (testing "Tests that it suggests the top-level of deeply nested trees."
    (is (contains? deeply-suggestions
                   "(+ (+ (+ (+ (+ 1 2) 3) 4) 5) 6)"))))