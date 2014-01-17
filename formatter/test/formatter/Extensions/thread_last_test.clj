(ns formatter.extensions.thread-last-test
  (:require [clojure.test :refer :all]
            [formatter.parser :as par]
			[formatter.extensions.thread-last :as ext]))

(defn get-suggestions [code]
  (:suggestions 
    ((:process-code ext/extension) 
      {:tree (par/parser code) 
       :suggestions []})))
(defn set-of-first [suggestions]
  (set (map #(first (clojure.string/split % #" ")) suggestions)))

(def clojuredocs-example 
  "(reduce + (take 10 (filter even? (map #(* % %) (range)))))")
(deftest test-clojuredocs-example
  (testing "Tests the threading example from clojuredocs"
    (is (= 1
           (count (get-suggestions clojuredocs-example))))))

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

;; Tests for:
;(html/defsnippet extensions-list "landing.html" *extensions-sel*
;  [extension]
;  [:a] (html/do->
;         (html/content (:description extension))
;         (html/set-attr :href (:url extension))))
; There are other defsnippets that could *possibly* be picked up if code is wrong! It should only pick up this one, though.
(deftest html-defsnippet
  (testing "Tests that it picks up html/defsnippet (level 4 nesting)."
    (is
      (= 1
         (->> styler-suggestions
              (map #(first (clojure.string/split % #" ")))
              (filter #(= "(html/defsnippet" %))
              (count))))))

;; Test to see whether we can rebind *do-not-nest*
(binding [ext/*do-not-nest* (conj ext/*do-not-nest* "html/defsnippet")]
  (let [binding-suggestions (set-of-first (get-suggestions code-styler))]

    (deftest binding-exclusion
      (testing "Checks to see that rebound do-not-nest excludes."
        (is (not
          (contains? binding-suggestions "(html/defsnippet")))))))