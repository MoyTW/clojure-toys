(ns wysitutwyg.routes.corpora
  (:import java.io.File)
  (:use [ring.util.response :only [response]])
  (:require [compojure.core :as compojure]
            [ring.middleware.json :as mid-json]
            [clojure.data.json :as json]
            [wysitutwyg.hal.hal-core :as hal]))

;;;; Returns info on all corpora.
;;;;   A lot of placeholders here since at some point I will move away from 
;;;; this directory-as-database structure and put in a Proper Database, from
;;;; which things like the size, description, etc, can be stored and retrieved.

;; hahaha my database is the filesystem :(
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

(def all-parses 
  (->> corpora-names
       (map #(hash-map % (get-parse-info %)))
       (apply merge)))

(def corpora-embedded
  (->> corpora-names
       (map #(-> (hal/new-resource (str "/corpora/" %))
                 (hal/add-properties :name % 
                                     :size "Placeholder"
                                     :description "Placeholder")))))

(def hal-all-corpora
  (-> (hal/new-resource "/corpora")
      (hal/add-properties :num-corpora (count corpora-embedded))
      (hal/add-embedded-set "corpora" corpora-embedded)))

; Awkward!
; Not used anymore, but if it were to be used, is this the best way to gen?
(defn get-parses
  [name]
  (let [parses ((all-parses name) "targets")]
    ;; Bad: self here isn't, uh, self, it's the json parse.
    (->> (map #(str "/corpora/" name "/" (% "outfile")) parses)
         (map hal/new-resource)
         (map #(hal/add-property-map %2 %1) parses)))) ; This is awkward

(defn hal-corpus
  [name]
  (if ((into #{} corpora-names) name)
      (-> (hal/new-resource (str "/corpus/" name))
          (hal/add-properties :parses ((all-parses name) "targets"))
          #_(hal/add-embedded-set :parses (get-parses name))
          (response))
      {:status 404 :body (str "No such corpus, " name)}))

(compojure/defroutes routes
  (-> (compojure/context "/corpora" []
        (compojure/GET "/" [] (response hal-all-corpora))
        (compojure/GET "/:name" [name] (hal-corpus name)))
      (mid-json/wrap-json-response)))