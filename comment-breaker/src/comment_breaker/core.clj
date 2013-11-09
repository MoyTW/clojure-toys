(ns comment-breaker.core
  (:gen-class))
(use '[clojure.tools.cli :only[cli]])

(def line-length (atom 80))
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

;; Should catch exceptions on I/O!
(defn -main
  [& args]
  (let [cli-vec 
          (cli args
               ["-i" "--in-file" "File to run comment processing on." 
                 :default nil]
               ["-o" "--out-file" "Output file." :default nil]
               ["-l" "--line-length" "The desired line length." 
                 :default 80 :parse-fn #(Integer. %)])
        cli-map (first cli-vec)
        ifile (:in-file cli-map)
        ofile (if-let [o (:out-file cli-map)] o	
                (suffix-file ifile "-broken"))]		; causes a NPE if no in!
    (if-not ifile (prn (last cli-vec))
      (do
        (reset! line-length (:line-length cli-map))
        (spit ofile (process-file (slurp ifile)))))))