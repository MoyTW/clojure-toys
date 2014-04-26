(ns wysitutwyg.routes.transformer
  (:import java.io.File)
  (:require [net.cgrand.enlive-html :as html]
            [compojure.core :as compojure]
            [clojure.data.json :as json]
            [wysitutwyg.markov.textgen :as markov])
  (:use ring.util.response))

(def corpora-root (File. "resources/public/corpora"))
(def json-form
"{
  \"corpus\":\"sonnets\"
  \"file\":\"1.json\"
  \"body\":\"foo bar baz\"
}")

(defn describe
  []
  {:status 405
   :body (str "JSON form: " json-form)})

(defn find-file
  "Given a directory, finds the file with a matching name, or empty seq."
  [dir name]
  (if (= (type dir) java.io.File)
      (->> dir
           (.listFiles)
           (filter #(= name (.getName %)))
           (first))))

(defn retrieve-datamap
  [corpus file]
  (if-let [f (find-file (find-file corpora-root corpus) file)]
    (markov/parse-into-datamap (slurp f))))

;;; TODO: Implement this.
(defn transform
  [datamap body]
  body)

;;; Return the text, transformed by the appropriate datamap.
;;; TODO: Ugly.
(defn handle-transform-request
  [json-str]
  (let [{:keys [corpus file body]} (json/read-str json-str :key-fn keyword)]
    (if-not (and corpus file body)
      {:status 400}
      (if-let [datamap (retrieve-datamap corpus file)]
        (str (transform datamap body))
        {:status 404
         :body (str "Could find targeted file: " corpus "/" file)}))))

(compojure/defroutes routes
  (compojure/GET "/transformer" [] (describe))
  (compojure/POST "/transformer" 
                  {body :body}
                  (handle-transform-request (slurp body))))