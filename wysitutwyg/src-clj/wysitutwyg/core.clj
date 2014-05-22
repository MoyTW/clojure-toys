(ns wysitutwyg.core
  (:use compojure.core)
  (:require [ring.middleware.stacktrace :as mid-stacktrace]
            [ring.middleware.json :as mid-json]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [compojure.response :as response]
            [wysitutwyg.routes.landing :as landing]
            [wysitutwyg.routes.transformer :as transformer]
            [wysitutwyg.routes.corpora :as corpora]))

(defroutes main-routes
  landing/routes
  transformer/routes
  corpora/routes
  (route/resources "/res")
  (route/not-found "Page not found"))

(def app
  (-> (handler/site main-routes)
      (mid-stacktrace/wrap-stacktrace)
      (mid-json/wrap-json-response)
      (mid-json/wrap-json-body)))