(ns learning-ring.core
  (:require [compojure.core :as compojure]
            [learning-ring.routes.styler :as styler]
            [learning-ring.routes.coredocs :as coredocs])
  (:use net.cgrand.enlive-html
        ring.adapter.jetty
        ring.util.response
        ring.middleware.params
        ring.middleware.reload
        ring.middleware.stacktrace))
    
(compojure/defroutes app-routes
  styler/routes
  coredocs/routes)

(def app
  (-> app-routes
      (wrap-reload)
      (wrap-stacktrace)
      (wrap-params)))

(defn boot[]
  (ring.adapter.jetty/run-jetty #'app {:port 3000}))