(ns wysitutwyg.routes.corpora
  (:import java.io.File)
  (:use [ring.util.response :only [response]])
  (:require [compojure.core :as compojure]
            [clojure.data.json :as json]))

;;;; Returns info on all corpora.

(def root (File. "resources/public/corpora"))
(def corpora-names
  (->> (.listFiles root)
       (map #(.getName %))))

(defn get-parse-info
  [corpus-name]
  (->> (str root "/" corpus-name)
       (File.)
       (.listFiles)
       (filter #(= "parses.json" (.getName %)))
       (first)
       (slurp)
       (json/read-str)))

(def all-parses (map get-parse-info corpora-names))

(def rest-response
  {:_embedded all-parses
   :_links {:self "link"}})

(compojure/defroutes routes
  (compojure/context "/corpora" []
    (compojure/GET "/" [] (response rest-response))
    (compojure/GET "/:id" [id] (response all-parses))))