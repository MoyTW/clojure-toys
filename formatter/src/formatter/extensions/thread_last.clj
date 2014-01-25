(ns formatter.extensions.thread-last
  (:require [instaparse.core :as insta]
            [formatter.parser :as par]
			[formatter.extensions.thread-aux :as thread]))

(def ^:dynamic *threading-params*
  {:selection-function last
   :min-depth 3
   :max-branch-depth 2
   :do-not-nest #{"defn" "loop" "let" "if" "and" "or" "fn" "deftest" "is" "testing"}})

(def extension
  {:name "thread-last"
   :description "thread-last adds to suggestions possible locations to use ->>"
   :url ""
   :is-active true
   :process-code #(thread/process-code *threading-params* %)})