(defproject formatter "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/tools.cli "0.2.4"]
                 [instaparse "1.2.10"]
                 [clj-diff "1.0.0-SNAPSHOT"]]
  :main ^:skip-aot formatter.core
  :target-path "target/%s")
