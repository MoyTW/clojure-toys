(ns wysitutwyg.routes.landing
  (:require [net.cgrand.enlive-html :as html]
            [compojure.core :as compojure])
  (:use ring.util.response))

;; This should correspond to actual, well, corpora.
(def my-corpora 
  [{:name "Shakespeare's Sonnets", :location "res/corpora/sonnets/"} 
   {:name "Translation of Lorem Ipsum", :location "res/corpora/loremipsum/"}])

(html/deftemplate landing "pages/landing.html" [corpora]
  [:head] (html/append (first (html/html [:script {:src "res/js/main.js"}])))
  [:select#corpus-selection :option]
    (html/clone-for [corpus corpora]
                    [:option] (html/content (corpus :name))
                    [:option] (html/set-attr :value (corpus :location)))
  [:select#corpus-selection [:option html/first-of-type]] (html/set-attr :selected "selected"))

(compojure/defroutes routes
  (compojure/GET "/" [] (landing my-corpora)))