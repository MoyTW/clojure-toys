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

(def ^:dynamic *suggestions-sel* [:.suggestions-list :> html/first-child])
(html/defsnippet suggestions-list "landing.html" *suggestions-sel*
  [suggestion]
  [:li] (html/content suggestion))
  
(html/deftemplate landing "landing.html" [old-code new-code changes suggestions e-list]
  [:.extensions] (html/content (map #(extensions-list %) e-list))
  [:.changes-list] (html/content (map #(changes-list %) changes))
  [:.suggestions-list] (html/content (map #(suggestions-list %) suggestions))
  [:.code-input] (html/content old-code)
  [:.code-output] (html/content new-code))

(defn resolve-landing [code]
  (let [result (f/apply-all-extensions (par/parser code))]
    (response (apply str (landing code (:text result) (:changes result) (:suggestions result) f/extensions)))))
  
(compojure/defroutes routes
  (compojure/GET "/styler" [] (response (apply str (landing "" "" [] [] f/extensions))))
  (compojure/POST "/styler" {{code "code"} :params} (resolve-landing code)))