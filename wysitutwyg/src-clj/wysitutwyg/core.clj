(ns wysitutwyg.core
  (:use compojure.core
        ring.middleware.reload
        ring.middleware.stacktrace
        ring.middleware.params)
  (:require [compojure.route :as route]
            [compojure.handler :as handler]
            [compojure.response :as response]
            [wysitutwyg.routes.landing :as landing]
            [wysitutwyg.routes.transformer :as transformer]))

(defroutes main-routes
  landing/routes
  transformer/routes
  (route/resources "/res")
  (route/not-found "Page not found"))

(def app
  (-> (handler/site main-routes)
      (wrap-params)
      (wrap-reload)
      (wrap-stacktrace)))