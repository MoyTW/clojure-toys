(ns formatter.extensions.thread-first
  (:require [instaparse.core :as insta]
            [formatter.parser :as par]))

(defn process-code [params]
  params)

(def extension
  {:description "thread-first adds to suggestions possible locations to use ->"
   :url ""
   :is-active true
   :process-code process-code})