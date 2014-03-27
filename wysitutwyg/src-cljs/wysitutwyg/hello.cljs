(ns wysitutwyg.hello
  (:require [wysitutwyg.textgen :as textgen]
            [wysitutwyg.markov :as markov]
            [clojure.browser.net :as net]
            [goog.net.XhrIo :as xhrio]))

(def interval 1000)

(def self-url (str (.-protocol (.-location js/window)) "//" (.-host (.-location js/window))))

(def corpus-info (atom {:location nil :n nil :counts nil}))

(defn by-id
  "Utility function for getElementById."
  [id]
  (.getElementById js/document id))
   
;; Unreasonably slow with a corpus of any non-trivial size.
;; TODO: Caching doesn't take into account which corpus!
(defn create-sonnet [info]
  (let [output (textgen/gen-from-text (.-value (by-id "inputtext"))
                                      (:counts info))]
    (set! (.-innerHTML (by-id "outputtext")) output)))

;; Horribly constructed, and doesn't hold off of trying to build a new list
;; while it's still getting info from the server
(defn get-corpus-info []
  (let [corpus-selection (by-id "corpus-selection")
        n-selection (by-id "n-selection")]
    {:location (.-value corpus-selection)
     :n (.-value n-selection)}))

;; Need to build in some blocking so that it doesn't gen with old while
;; getting new from the server
(defn get-json [info]
  (xhrio/send (str self-url "/" (:location info) (:n info) ".json")
              #(swap! corpus-info
                      assoc
                      :counts
                      (->> (.-target %)
                           (.getResponseJson)
                           (js->clj)
                           (into {}))
                      :n (:n info)
                      :location (:location info))))

;; TODO: Make this less terrible
(defn change-corpus []
  (let [{:keys [location n] :as info} (get-corpus-info)]
    (if-not (and (= n (:n @corpus-info)) 
                 (= location (:location @corpus-info)))
            (get-json info))))

(defn onload []
  (do
    (change-corpus)
    (js/setInterval (create-sonnet @corpus-info) interval)))

(set! (.-onload js/window) onload)


;;; How can we make the text generation more responsive?