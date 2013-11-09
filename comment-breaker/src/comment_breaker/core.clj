(ns comment-breaker.core
  (:gen-class))
(use '[clojure.tools.cli :only[cli]])

(def line-length (atom nil))
(def sep-char (atom "\r\n"))

(defn suffix-file [fstr suffix]
  (let [splits (clojure.string/split fstr #"\.")]
    (str (apply str (butlast splits)) suffix "." (last splits))))
      
(defn build-line [lines nxt]
  (if (> (+ (count (last lines)) (inc (count nxt))) @line-length)
    (conj lines (str ";; " nxt))
    (conj (into [] (butlast lines)) (str (last lines) \space nxt))))
    
(defn eat-line [line]
  (if (and (> (count line) @line-length) (.startsWith line ";;"))
    (clojure.string/replace-first
      (clojure.string/join @sep-char
        (reduce build-line [""] (clojure.string/split line #"\s")))
      #"\s\;\;\s+"
      ";;   ")
    line))
    
(defn process-file [contents]
  (clojure.string/join @sep-char
    (map eat-line (clojure.string/split contents (re-pattern @sep-char)))))

(defn get-args [args]
  (let [parsed
          (cli args 
		    ["-i" "--in-file" "File to run comment processing on."]
			["-o" "--out-file" "Output file."]
			["-l" "--line-length" "The desired line length." 
			  :default 80 :parse-fn #(Integer. %)])
	    p-map (first parsed)]
	(do
	  (reset! line-length (:line-length p-map))
	  (cond 
	    (not (:in-file p-map))
	      (do (prn (last parsed)) (System/exit 0))
	    (not (:out-file p-map))
	      (assoc p-map :out-file (suffix-file (:in-file p-map) "-broken"))
	    :else p-map))))

(defn -main
  [& args]
  (let [cli-map (get-args args)]
    (try
      (spit (:out-file cli-map) (process-file (slurp (:in-file cli-map))))
	  (catch java.io.IOException e
	    (prn (.toString e))))))