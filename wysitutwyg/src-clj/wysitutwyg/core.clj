(ns wysitutwyg.core
  (:use compojure.core
        ring.middleware.reload
        ring.middleware.stacktrace)
  (:require [compojure.route :as route]
            [compojure.handler :as handler]
            [compojure.response :as response]
            [wysitutwyg.routes.landing :as landing]))

(defroutes main-routes
  landing/routes
  (route/resources "/res")
  (route/not-found "Page not found"))

(def app
  (-> (handler/site main-routes)
      (wrap-reload)
      (wrap-stacktrace)))