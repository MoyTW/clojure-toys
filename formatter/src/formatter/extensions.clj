(ns formatter.extensions
  (:require [formatter.extensions.comment-length :as comment-length]
            [formatter.extensions.when :as when]
			[formatter.extensions.when-not :as when-not]
            [formatter.extensions.thread-last :as thread-last]))

(def all-extensions 
  [comment-length/extension
   when/extension
   when-not/extension
   thread-last/extension])