(ns formatter.extensions.comment-length
  (:use formatter.extension))
  
(def max-length 80)
  
(defn should-break? [comment-str]
  (and (> (count comment-str) max-length)
       (= (first comment-str) \;))))

(defn build-line [lines nxt]
  (let [last-line (last lines)
        line-str (last last-line)]
  (if (> (+ (count line-str) (inc (count nxt))) max-length)
    (conj lines [:Comment "\n" ";" (apply str "; " nxt)])
    (assoc-in lines 
              [(dec (count lines)) (dec (count last-line))] 
              (apply str line-str " " nxt)))))

;; Converts comment into words
;; Reduces over words to build the lines
(defn break [comment-str]
  (let [words (clojure.string/split comment-str #"\s+")
        filtered-words (filter #(not= ";;" %) words)]
    (reduce build-line 
            [[:Comment ";" ";  "]]
            filtered-words)))
       
(defn process-comment [[k & vals]]
  (let [comment-str (apply str vals)]
    (if (should-break? comment-str)
        (break comment-str)
        [[k vals]])))
  
(defn find-comment [tree]
  (letfn [(reduce-tree [tree node]
            (cond
              (and (vector? node) (= (first node) :Comment)) (apply conj tree (process-comment node))
              (vector? node) (conj tree (find-comment node))
              :else (conj tree node)))]
  (reduce reduce-tree [] tree)))
          
(reify FormatterExtension
  (is-active [this] true)
  (modify-tree [this tree] (find-comment tree)))