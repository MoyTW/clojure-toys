(ns learning-ring.routes.coredocs
  (:require [net.cgrand.enlive-html :as html]
            [net.cgrand.xml :as xml]
            [compojure.core :as compojure])
  (:use ring.util.response))

(html/deftemplate query "query.html" [])
(html/deftemplate function-not-found "function_not_found.html" [fname]
  [:p#fname] (html/content fname))
(html/deftemplate clojuredoc "clojuredoc_response.html" [fname fform fdesc]
  [:p#fname] (html/content fname)
  [:p#fform] (html/content fform)
  [:p#fdesc] (html/content fdesc))

(defn fetch-url [url]
  (html/html-resource (java.net.URL. url)))

;; A copy of html/read, but breaks up the text with line breaks
(defn read-text [node]
  (cond
    (string? node) (str node "<br>")
    (xml/tag? node) (apply str (map read-text (:content node)))
    :else ""))

(defn read-documentation [fname]
  (map read-text
       (html/select (fetch-url (str "http://clojuredocs.org/clojure_core/clojure.core/" fname))
                    [:.doc :.content])))

(defn query-docs [name]
  (try (-> (read-documentation name)
           (response)
           (content-type "text/html"))
       (catch java.io.FileNotFoundException e
              (response (function-not-found name)))))

(defn resolve-get [name]
  (if name
      (query-docs name)
      (response (query))))

(compojure/defroutes routes
  (compojure/GET "/coredocs" [name] (resolve-get name)))