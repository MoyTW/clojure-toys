(ns formatter.extensions.thread-first
  (:require [instaparse.core :as insta]
            [formatter.parser :as par]
			[formatter.extensions.thread-aux :as thread]))

(def ^:dynamic *threading-params*
  {:thread-pred first
   :min-depth 3
   :max-branch-depth 2
   :do-not-nest #{}})

(def extension
  {:name "thread-first"
   :description "thread-first adds to suggestions possible locations to use ->"
   :url ""
   :is-active true
   :process-code #(thread/process-code *threading-params* %)})