(ns wysitutwyg.routes.corpora
  (:import java.io.File)
  (:use korma.core
        [ring.util.response :only [response]])
  (:require [wysitutwyg.db.core :as db]
            [compojure.core :as compojure]
            [ring.middleware.json :as mid-json]
            [clojure.data.json :as json]
            [wysitutwyg.hal.hal-core :as hal]))

;;;; Returns info on all corpora.
;;;;   A lot of placeholders here since at some point I will move away from 
;;;; this directory-as-database structure and put in a Proper Database, from
;;;; which things like the size, description, etc, can be stored and retrieved.

(defn build-embedded-corpora
  "Queries the database to build the embedded corpora resources."
  []
  (->> (select db/corpus (fields :id :name :description :word_count))
       (map #(-> (hal/new-resource (str "/corpora/" (:id %)))
                 (hal/add-property-map %)))))

(def hal-all-corpora
  (let [corpora (build-embedded-corpora)]
    (-> (hal/new-resource "/corpora")
        (hal/add-link :find "/corpora/{?id}" :templated true)
        (hal/add-property :num-corpora (count corpora))
        (hal/add-embedded-set "corpora" (build-embedded-corpora)))))

(defn select-corpus
  "Queries the database for corpus + parses."
  [id]
  (first
    (select db/corpus
      (where {:id id})
      (fields :id :name :description :word_count)
      (with db/parse
        (fields :id :name :arity :delimiter_regex)
        (with db/end_string (fields :string))))))

;;; This is a horrifying function
;;; TODO: Un-horrify
(defn format-parses
  "Formats end_string element of parses."
  [parses]
  (->> parses
       (map #(dissoc % :id))
       (map #(update-in % [:end_string] (fn [c] (map vals c)))) ;lol
       (map #(update-in % [:end_string] flatten))
       (map #(clojure.set/rename-keys % {:end_string :end_strings}))))

;;; TODO: Proper error response in json and such
(defn hal-corpus
  "Converts corpus to hal representation."
  [id]
  (if-let [{:keys [parse] :as data} (select-corpus id)]
    (-> (hal/new-resource (str "/corpora/" id))
        (hal/add-link :list "/corpora")
        (hal/add-link :find "/corpora/{?id}" :templated true)
        (hal/add-property-map (dissoc data :parse))
        (hal/add-property :parses (format-parses parse))
        (response))
    {:status 404 :body (str "No such corpus " id)})) 

(compojure/defroutes routes
  (-> (compojure/context "/corpora" []
        (compojure/GET "/" [] (response hal-all-corpora))
        (compojure/GET "/:id" [id] (hal-corpus id)))
      (mid-json/wrap-json-response)))