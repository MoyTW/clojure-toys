(ns wysitutwyg.db.init
  (use korma.core)
  (require [wysitutwyg.db.core :as db]))

;;; Run the sql contained in setup.sql, which creates the appropriate tables.
(def setup-sql
  (-> (slurp "resources/db/setup.sql")
      (clojure.string/split #";")))
(doall (map #(exec-raw % :keys) setup-sql))

;;; Populate the tables with the initial corpora.
(db/add-corpus "Shakespeare's Sonnets"
               "Every known sonnet written by Shakespeare."
               (slurp "resources/db/init/sonnets_corpus.txt"))
(db/add-parse
  {:corpus-name "Shakespeare's Sonnets"
   :name "Arity 1"
   :arity 1,
   :end-strings ["\n"]
   :delimiter-regex " "
   :json (slurp "resources/db/init/sonnets_1.json")})
(db/add-parse
  {:corpus-name "Shakespeare's Sonnets"
   :name "Arity 2"
   :arity 2,
   :end-strings ["\n"]
   :delimiter-regex " "
   :json (slurp "resources/db/init/sonnets_2.json")})
(db/add-parse
  {:corpus-name "Shakespeare's Sonnets"
   :name "Arity 3"
   :arity 3,
   :end-strings ["\n"]
   :delimiter-regex " "
   :json (slurp "resources/db/init/sonnets_3.json")})

(db/add-corpus "English Lorem Ipsum"
               "I never know Lorem Ipsum was an angry rant about pain."
               (slurp "resources/db/init/loremipsum_corpus.txt"))
(db/add-parse
  {:corpus-name "English Lorem Ipsum"
   :name "Arity 1"
   :arity 1,
   :end-strings ["." "!" "?"]
   :delimiter-regex " "
   :json (slurp "resources/db/init/loremipsum_1.json")})
(db/add-parse
  {:corpus-name "English Lorem Ipsum"
   :name "Arity 2"
   :arity 2,
   :end-strings ["." "!" "?"]
   :delimiter-regex " "
   :json (slurp "resources/db/init/loremipsum_2.json")})
(db/add-parse
  {:corpus-name "English Lorem Ipsum"
   :name "Arity 3"
   :arity 3,
   :end-strings ["." "!" "?"]
   :delimiter-regex " "
   :json (slurp "resources/db/init/loremipsum_3.json")})