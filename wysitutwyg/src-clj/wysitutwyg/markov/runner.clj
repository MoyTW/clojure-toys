(ns wysitutwyg.markov.runner
  (:import java.io.File)
  (:require [clojure.data.json :as json]
            [wysitutwyg.markov.parser :as parser]))

;;; Assumes a file structure of the following configuration:
;;; root
;;;  |-> corpus_1
;;;       |-> corpus.txt
;;;       |-> parses.json
;;;  |-> arbitrary_directory_name
;;;       |-> arbitrary_corpus_name.txt
;;;       |-> parses.json
;;;  |-> corpus_n
;;;       |-> corpus_n.txt
;;;       |-> parses.json
;;; where parses.json is a json file of the following form:
;;;   {"targets":
;;;    [{"arity":1,
;;;      "end-strings":[".", "!", "?"],
;;;      "delimiter-regex":" ",
;;;      "outfile":"1.json"},
;;;     {"arity":2,
;;;      "end-strings":[".", "!", "?"],
;;;      "delimiter-regex":" ",
;;;      "outfile":"2.json"}],
;;;    "corpus":"corpus.txt"}
;;; with each member of the "targets" array representing a request that the
;;; corpus be parsed to the destination, including parsing parameters.

(defn- read-json-str
  "Function to map from json to a Clojure map. Keywordizes, places end-strings
  into a set."
  [string]
  (json/read-str string
                 :key-fn keyword
                 :value-fn #(if (= %1 :end-strings) (into #{} %2) %2)))
                 ; I bet they built something in for this that I'm not using.

(defn- get-dirs
  "Returns a sequence of File objects representing directories from the 
  indicated directory."
  [dir]
  {:pre (= (type dir) java.io.File)}
  (->> dir
       (.listFiles)
       (filter #(.isDirectory %))))

(defn- find-file
  "Given a directory, finds the file with a matching name, or empty seq."
  [dir name]
  {:pre (= (type dir) java.io.File)}
  (->> dir
       (.listFiles)
       (filter #(= name (.getName %)))))

(defn- resolve-parse
  "Resolves a single parse request, outputting to filesystem."
  [corpus path {:keys [outfile] :as parse}]
  (prn (str "    Executing parse, writing to: " outfile))
  (with-open [w (clojure.java.io/writer (str path File/separator outfile))]
    (spit w (json/write-str (parser/parse-counts corpus parse)))))

(defn- resolve-parses
  "Takes a map of requests and parses and outputs all results."
  [{:keys [path targets corpus]}]
  {:pre (= (type path) java.io.File)}
  (prn (str "Parsing in folder " path ":"))
  (prn "Target File: " corpus)
  (let [corpus-text (slurp (str path File/separator corpus))]
    (doall (map #(resolve-parse corpus-text path %) targets))))

(defn run-all-parses
  "Finds all parses.json files in subdirectories of path, and resolves the
  parse requests according to their contents."
  [path name]
  {:pre [(or (= (type path) java.lang.String) (= (type path) java.io.File))]}
  (->> (if (= java.lang.String (type path)) 
           (File. path)
           path)
       (get-dirs)
       (mapcat #(find-file % name))
       (map #(assoc (read-json-str (slurp %))
                    :path
                    (.getParentFile %)))
       (map resolve-parses)
       (dorun)))

(def corpora-path "resources/public/corpora")
(def json-name "parses.json")
(run-all-parses corpora-path json-name)