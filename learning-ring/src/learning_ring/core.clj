(ns learning-ring.core
  (:require [net.cgrand.enlive-html :as html]
            [net.cgrand.xml :as xml]
			[formatter.core :as f]
			[formatter.parser :as par])
  (:use net.cgrand.enlive-html
        ring.adapter.jetty
        ring.util.response
        ring.middleware.params
        ring.middleware.reload
        ring.middleware.stacktrace))

(def ^:dynamic *extensions-sel* [:.extensions :> html/first-child])
(html/defsnippet extensions-list "landing.html" *extensions-sel*
  [extension]
  [:a] (html/do->
         (content (:description extension))
         (set-attr :href (:url extension))))

(def ^:dynamic *changes-sel* [:.changes-list :> html/first-child])
(html/defsnippet changes-list "landing.html" *changes-sel*
  [change]
  [:li] (html/content change))

(html/deftemplate landing "landing.html" [old-code new-code changes e-list]
  [:.extensions] (html/content (map #(extensions-list %) e-list))
  [:.changes-list] (html/content (map #(changes-list %) changes))
  [:.code-input] (html/content old-code)
  [:.code-output] (html/content new-code))
(html/deftemplate formatted-code "formatted_code.html" [code]
  [:textarea#display] (html/content code))
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
  (try (-> (response name)
           (content-type "text/html"))
       (catch java.io.FileNotFoundException e
              (response (function-not-found name)))))

;; Hmm. The (= name "") doesn't work - you can enter spaces, and it goes to...http://clojuredocs.org/clojure_core/clojure.core/! And that's valid, so it doesn't get a FileNotFound, but read-documentation returns nothing.
(defn handler [{{code "code" name "name"} :params}]
  (cond
    code
      (let [[new-code changes] (f/apply-all-extensions [(par/parser code) []])]
        (response (apply str (landing code new-code changes f/extensions))))
    (= name "") (response "You have entered...nothing. Hit back and try again.")
    (= name nil) (response (apply str (landing "" "" [] f/extensions)))
    :else (query-docs name)))

(def app
  (-> handler
      (wrap-reload)
      (wrap-stacktrace)
      (wrap-params)))

(defn boot[]
  (ring.adapter.jetty/run-jetty #'app {:port 3000}))