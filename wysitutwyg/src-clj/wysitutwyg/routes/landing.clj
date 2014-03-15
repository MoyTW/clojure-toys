(ns wysitutwyg.routes.landing
  (:require [net.cgrand.enlive-html :as html]
            [compojure.core :as compojure])
  (:use ring.util.response))

(html/deftemplate landing "pages/landing.html" []
  [:head] (html/append (first (html/html [:script {:src "res/js/main.js"}]))))

(compojure/defroutes routes
  (compojure/GET "/" [] (landing)))