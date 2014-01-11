(ns learning-ring.routes.styler
  (:require [net.cgrand.enlive-html :as html]
            [compojure.core :as compojure]
            [formatter.core :as f]
            [formatter.parser :as par])
  (:use ring.util.response))

(def ^:dynamic *extensions-sel* [:.extensions :> html/first-child])
(html/defsnippet extensions-list "landing.html" *extensions-sel*
  [extension]
  [:a] (html/do->
         (html/content (:description extension))
         (html/set-attr :href (:url extension))))

(def ^:dynamic *changes-sel* [:.changes-list :> html/first-child])
(html/defsnippet changes-list "landing.html" *changes-sel*
  [change]
  [:li] (html/content change))
  
(html/deftemplate landing "landing.html" [old-code new-code changes e-list]
  [:.extensions] (html/content (map #(extensions-list %) e-list))
  [:.changes-list] (html/content (map #(changes-list %) changes))
  [:.code-input] (html/content old-code)
  [:.code-output] (html/content new-code))

(defn resolve-landing [code]
  (let [[new-code changes] (f/apply-all-extensions [(par/parser code) []])]
    (response (apply str (landing code new-code changes f/extensions)))))
  
(compojure/defroutes routes
  (compojure/GET "/styler" [] (response (apply str (landing "" "" [] f/extensions))))
  (compojure/POST "/styler" {{code "code"} :params} (resolve-landing code)))