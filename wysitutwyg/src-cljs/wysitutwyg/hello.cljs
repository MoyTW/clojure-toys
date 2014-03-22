(ns wysitutwyg.hello
  (:require [wysitutwyg.textgen :as textgen]
            [wysitutwyg.markov :as markov]
            [clojure.browser.net :as net]
            [goog.net.XhrIo :as xhrio]))

(def interval 1000)

(def self-url (str (.-protocol (.-location js/window)) "//" (.-host (.-location js/window))))

(def corpus-counts (atom ""))

;; bah! This parsing action takes too long. Parse it on the server, and provide it as a resource!
#_(defn set-corpus-counts [corpus]
  (reset! corpus-counts (markov/parse-counts 2 corpus)))

(defn set-corpus-counts [corpus]
  (reset! corpus-counts corpus))
  
(defn by-id
  "Utility function for getElementById."
  [id]
  (.getElementById js/document id))
   
;; Unreasonably slow with a corpus of any non-trivial size.
(defn create-sonnet [counts]
  (let [output (textgen/gen-from-text (.-value (by-id "inputtext"))
                                      counts)]
    (set! (.-value (by-id "outputtext")) output)))

;; Retrieves the text of the corpus from the server
;; TODO: Hardcoded test file should be choosable!
(xhrio/send (str self-url "/res/corpora/sonnets/onegram.json") 
            #(reset! corpus-counts 
                     (->> (.-target %) 
                          (.getResponseJson) 
                          (js->clj) 
                          (into {}))))

(js/setInterval #(create-sonnet @corpus-counts) interval)