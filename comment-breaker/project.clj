(defproject comment-breaker "0.1.0-SNAPSHOT"
  :description "A short script to break comments over 80 lines."
  :url "http://example.com/FIXME"
  :license {:name "MIT License (2013, Travis Moy)"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/tools.cli "0.2.4"]]
  :main ^:skip-aot comment-breaker.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
