(ns formatter.extensions.thread-last-test
  (:require [clojure.test :refer :all]
            [formatter.parser :as par]
			[formatter.extensions.thread-last :as ext]))

(defn get-suggestions [code]
  (map :code
       (:suggestions 
         ((:process-code ext/extension) 
           {:tree (par/parser code) 
           :suggestions []}))))
(defn set-of-first [suggestions]
  (set (map #(first (clojure.string/split % #" ")) suggestions)))

(def clojuredocs-example 
  "(reduce + (take 10 (filter even? (map #(* % %) (range)))))")
(deftest test-clojuredocs-example
  (testing "Tests the threading example from clojuredocs"
    (is (= 1
           (count (get-suggestions clojuredocs-example))))))

;;;; Using styler code in Resources:
(def code-styler (slurp "test/resources/styler_20140115.txt"))
(def styler-suggestions (get-suggestions code-styler))
(def first-suggestions (set-of-first styler-suggestions))

;; We want to test that the following is NOT considered for threading:
;(compojure/defroutes routes
;  (compojure/GET "/styler" [] (response (apply str (landing "" "" [] [] f/extensions))))
;  (compojure/POST "/styler" {{code "code"} :params} (resolve-landing code)))
(deftest defroutes-not-selected
  (testing "Tests that defroutes (nested in both parameters) is not selected."
    (is (not
      (contains? first-suggestions "(compojure/defroutes")))))

;; We want to test that the following IS considered for threading:
;(compojure/GET "/styler" [] (response (apply str (landing "" "" [] [] f/extensions))))
;; Can be expressed as:
;(->> (landing "" "" [] [] f/extensions)
;     (apply str)
;     (response)
;     (compojure/GET "/styler" []))
(deftest get-is-selected
  (testing "Tests that GET (level 5 nesting) is selected."
    (is 
      (contains? first-suggestions "(compojure/GET"))))

;;;; WHY DO WE EVEN WANT TO PICK THIS ONE UP!?
;;;; It's not threadable in the least!
;;;; Changed it to *does not* pick it up.
;; Tests for:
;(html/defsnippet extensions-list "landing.html" *extensions-sel*
;  [extension]
;  [:a] (html/do->
;         (html/content (:description extension))
;         (html/set-attr :href (:url extension))))
(deftest html-defsnippet
  (testing "Tests that it *doens't* pick up html/defsnippet (L 3 non-last)."
    (is
      (= 0
         (->> styler-suggestions
              (map #(first (clojure.string/split % #" ")))
              (filter #(= "(html/defsnippet" %))
              (count))))))

;;;; Using split-chain
(def split-chain "(+ 1 (- 2 (- 3 (+ 4 (- 5 6))) (+ 7 (- 8 (+ 9 (+ 10 11))))))")
(def split-suggestions (into #{} (get-suggestions split-chain)))

(deftest does-not-suggest-multipath-calls
  (testing "Tests that it doesn't suggest calls which have split paths"
    (is (not (contains? split-suggestions split-chain)))))

(deftest suggests-subtrees
  (testing "Tests that it does suggest subtrees inside a non-threading node"
    (is (and (contains? split-suggestions "(- 3 (+ 4 (- 5 6)))")
             (contains? split-suggestions "(+ 7 (- 8 (+ 9 (+ 10 11))))")))))

;; Tests that if there's a long thread-tail, it only picks the highest-order thread
; So, (+ 1 (- 2 (- 3 (+ 4 (- 5 (+ 6 (+ 7 (- 8 (+ 9 (+ 10)))))))))) should only 
; return one result! It should not return, say, (- 8 (+ 9 (+ 10))).
(def long-chain "(+ 1 (- 2 (- 3 (+ 4 (- 5 (+ 6 (+ 7 (- 8 (+ 9 10)))))))))")
(deftest does-not-suggest-subresults
  (testing "Tests that it doesn't suggest sub-results."
    (is (= 1
           (count (get-suggestions long-chain))))))

;; Test to see whether we can rebind *do-not-nest*
#_(binding [ext/*do-not-nest* (conj ext/*do-not-nest* "html/defsnippet")]
  (let [binding-suggestions (set-of-first (get-suggestions code-styler))]

    (deftest binding-exclusion
      (testing "Checks to see that rebound do-not-nest excludes."
        (is (not
          (contains? binding-suggestions "(html/defsnippet")))))))

(deftest single-form
  (testing "Testing that it doesn't explode when passed a single form."
    (is (= 0 (count (get-suggestions ":a"))))))