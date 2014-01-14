(ns formatter.extensions.thread-last-test
  (:require [clojure.test :refer :all]
            [formatter.parser :as par]
			[formatter.extensions.when :as ext]))

(defn get-suggestions [code]
  (:suggestions 
    ((:process-code ext/extension) 
      {:tree (par/parser code) 
       :suggestions []})))

(def clojuredocs-example 
  "(reduce + (take 10 (filter even? (map #(* % %) (range)))))")
(deftest test-clojuredocs-example
  (testing "Tests the threading example from clojuredocs"
    (is (= 1
           (count (get-suggestions clojuredocs-example))))))