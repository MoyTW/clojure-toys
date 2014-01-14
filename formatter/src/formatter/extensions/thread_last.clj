(ns formatter.extensions.thread-last
  (:require [instaparse.core :as insta]
            [formatter.parser :as par]))

(defn process-code [params]
  params)

(def extension
  {:description "thread-last is not yet implemented."
   :url ""
   :is-active true
   :process-code process-code})