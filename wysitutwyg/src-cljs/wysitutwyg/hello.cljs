(ns wysitutwyg.hello
  (:require [wysitutwyg.textgen :as textgen]
            [wysitutwyg.markov :as markov]
            [clojure.browser.net :as net]
            [goog.net.XhrIo :as xhrio]
            [goog.structs :as structs]
            [goog.Uri.QueryData :as query]))

(def self-url (str (.-protocol (.-location js/window)) 
                   "//"
                   (.-host (.-location js/window))))

;{"corpus":"sonnets"
; "file":"1.json"
; "body":"foo bar baz"}
            
;;;; Transformer
(def transform-url (str self-url "/transformer"))

(def xhr (net/xhr-connection))
(def test-post-clojure
  "{\"corpus\":\"sonnets\"
    \"file\":\"1.json\"
    \"body\":\"foo bar baz z z z z\"}")
(def headers
  (-> {:Content-Type "application/json"}
      clj->js
      structs/Map.))

(defn callback
  [reply]
  (let [target (.-target reply)]
    (js/alert (str (.getLastError target) (.getStatus target) (.isSuccess target) (.getResponseText target)))))

(defn get-json []
  #_(xhrio/send transform-url callback)
  #_(xhrio/send transform-url callback "POST")
  (xhrio/send transform-url callback "POST" test-post-clojure headers))

;;;; Timers
(def typingInterval 1000)
(def timer nil)

(defn start-timer
  []
  (js/clearTimeout timer)
  (def timer (js/setTimeout get-json typingInterval)))