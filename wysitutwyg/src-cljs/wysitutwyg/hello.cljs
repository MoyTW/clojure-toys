(ns wysitutwyg.hello
  (:require [wysitutwyg.textgen :as textgen]
            [wysitutwyg.markov :as markov]
            [clojure.browser.net :as net]
            [goog.net.XhrIo :as xhrio]
            [goog.structs :as structs]
            [goog.Uri.QueryData :as query]))

;;; Find the URL of the page
(def self-url (str (.-protocol (.-location js/window)) 
                   "//"
                   (.-host (.-location js/window))))

(def input-name "inputtext")
(def output-name "outputtext")
(def corpus-options-name "corpus-selection")

(def transform-url (str self-url "/transformer"))
(def xhr (net/xhr-connection))

;;;; Utility
(defn by-id
  "Utility function for getElementById."
  [id]
  (.getElementById js/document id))

(defn to-json
  "Converts clojure data to json using JSON/stringify."
  [data]
  (JSON/stringify (clj->js data)))

;;;; Transformer
(def headers
  (-> {:Content-Type "application/json"}
      clj->js
      structs/Map.))

(defn build-request-body
  "Polls the HTML elements and builds a request from them."
  []
  (to-json
    {:corpus (.-value (by-id corpus-options-name))
     :file "1.json" ; TODO: Allow user to select
     :body (.-value (by-id input-name))}))

(defn callback
  [reply]
  (let [target (.-target reply)]
    (if-not (.isSuccess target)
            (js/alert (str (.getLastError target) (.getStatus target)))
            (set! (.-innerHTML (by-id output-name)) 
                  (.getResponseText target)))))

(defn request-text []
  (xhrio/send transform-url callback "POST" (build-request-body) headers))

;;;; Handle corpus change event
(defn onchange-corpus 
  "Refreshes the text."
  []
  (request-text))
  
;;;; Timers
(def typingInterval 1000)
(def timer nil)

(defn oninput-event
  "Clears the timer, if exists, and restarts it. Calls request-text when the
  timer expires."
  []
  (js/clearTimeout timer)
  (def timer (js/setTimeout request-text typingInterval)))