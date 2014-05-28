(ns wysitutwyg.db.core
  (use korma.core)
  (require [korma.db :as korma-db]))

;;;; korma note: There is a bug with exec-raw and sqlite3!
;;;; Passing in return option :keys will circumvent the issue.
;;;; See: https://github.com/korma/Korma/issues/197

;;;; korma note: There appears to be a bug with inserting multiple rows in one
;;;; request with sqlite!
;;;; In particular, this code formerly in add-parse will fail:
;;;; (insert end_string
;;;;   (values [{:parse_id 1 :string "what"} {:parse_id 1 :string "dafuq"}]))
;;
;; Failure to execute query with SQL:
;; INSERT INTO "end_string" ("string", "parse_id") VALUES (?, ?), (?, ?)  ::  [what 1 dafuq 1]
;; SQLException:
;;  Message: [SQLITE_ERROR] SQL error or missing database (near ",": syntax error)
;;  SQLState: null
;;  Error Code: 0
;; SQLException [SQLITE_ERROR] SQL error or missing database (near ",": syntax error)  org.sqlite.DB.newSQLException (DB.java:383)
;;
;;;; So, yeah, that's nasty.

(def db-map (korma-db/sqlite3 {:db "resources/db/db.sqlite3"}))
(korma-db/defdb default-db db-map)

(declare corpus parse end_string)

(defentity corpus
  (has-many parse))

(defentity parse
  (belongs-to corpus)
  (has-many end_string))

(defentity end_string
  (belongs-to parse))

(defn add-corpus
  [name description text]
  (insert corpus
    (values
      {:name name
       :description description
       :text text})))

;;; This is a pretty big function, huh.
(defn add-parse
  [{:keys [corpus-name name arity end-strings delimiter-regex json]}]
  (let [insert-id 
          ((comp second first)
            (insert parse 
              (values
                {:corpus_id ((comp :id first)
                              (select corpus
                                (fields :id)
                                (where {:name corpus-name})))
                 :name name
                 :arity arity
                 :delimiter_regex delimiter-regex
                 :json json})))]
    ;; Note: We cannot use the collection-insertion syntax with sqlite.
    (->> end-strings 
         (map #(hash-map :parse_id insert-id :string %))
         (map #(insert end_string (values %)))
         (doall))))