(ns wysitutwyg.markov.parser)

(def test-text "Me is mad! It is mad? It is angry. She is mad. He is sad! It is sad? She is sad. Ze mad? Ze sad? Ze jelly? Ha! He is mad! He is mad!")

;; I won't be winning any elegant code competitions...
(defn- split-by-string
  [string splitter]
  (loop [string string out []]
    (let [idx (.indexOf string splitter)]
      (if (>= idx 0)
          (let [e-idx (+ idx (count splitter))
                pre (if-let [p (seq (.substring string 0 idx))] [p])
                mid (if-let [m (seq (.substring string idx e-idx))] [m])
                suf (seq (.substring string e-idx))]
            (recur (apply str suf) (concat out pre mid)))
          (if (empty? string) 
              (vec (map #(apply str %) (vec out)))
              (conj (vec (map #(apply str %) out)) string))))))

(defn- split-step
  [out in]
  (mapcat #(split-by-string % in) out))
  
(defn- split-by-all
  [string splitters]
  (reduce split-step (split-by-string string (first splitters)) (rest splitters)))

(defn- process-corpus [corpus {:keys [delimiter-regex end-strings]}]
  "Processes the corpus into a collection of segments, determined by the
  delimiter set. Members of end-strings are grouped into their own segments."  
  (->> (.split corpus delimiter-regex)
       (vec) ; If there are no splits, String.split produces [corpus].
       (map #(apply str %))
       (mapcat #(split-by-all % end-strings))))

(defn- update-start
  "Updates iff the arity of the key is equal to n."
  [start n follows rest-corpus]
  ;TODO: ugly
  (if (= n 1)
      (update-in start [[follows]] (fnil inc 0))
      (let [key [(apply conj [follows] (take (dec n) (rest rest-corpus)))]] 
        (if (= n (apply count key))
            (update-in start key (fnil inc 0))
            start))))

(defn- build-output-map
  [start counts end-strings]
  (let [out-counts counts
        branch-factor (/ (reduce + (map (comp count second) counts))
                         (count counts))]
    {:branching-factor branch-factor
     :start start
     :end (map str end-strings)
     :counts out-counts}))

(defn- parse-step
  "Recursive step producing a map of the form 
  {:start {STATE0 COUNT0, STATE1 COUNT1, ... STATEn COUNTn}
   :end (ENDSTR0, ENDSTR1 ... ENDSTRn)
   :counts {STATE0 NEXT0, STATE1 NEXT1, ... STATEn NEXTn}}
  where STATEn is an array of strings
  COUNTn is an int
  NEXTn := [STATEn {STR0 COUNT0, STRn, COUNTn}]
  This isn't really good documentation; it'll confuse people who aren't me."
  [corpus arity end-strings]
  (loop [counts {} 
         start (update-start {} arity (first corpus) corpus)
         corpus corpus]
    (let [[state rest-corpus] (split-at arity corpus)
           follows (conj (vec (rest state)) (first rest-corpus))]
      (cond
        (and (seq rest-corpus) (end-strings (last state)))
          (recur (update-in counts [state follows] (fnil inc 0))
                 (update-start start arity (first rest-corpus) rest-corpus)
                 (rest corpus))
        (seq rest-corpus)
          (recur (update-in counts [state follows] (fnil inc 0))
                 start
                 (rest corpus))
        :else
          (build-output-map start counts end-strings)))))

;; Does not properly generate :start nodes for counts > 1!
;; Actually, it doesn't really know how to handle counts > 1 at all...hmm.
;; It's a bit of a monster function, too. Unwieldy, at best.
;; TODO: Refactor this?
;; TODO: Hey, it's missing the first one! That is - the very first. Hmm.
(defn parse-counts
  "Creates a mapping of states to counts of the form 
  {start-state {end-state n, end-state n}}
    start-state | ((\\w \\o \\r \\d \\1) (\\w \\2))
    end-state | (\\w \\o \\r \\d)
  with the start and end states mapped to :start and :end."
  [corpus {:keys [arity end-strings delimiter-regex] :as pattrs}]
  (let [processed (process-corpus corpus pattrs)]
    (parse-step processed
                arity
                end-strings)))

;;; What we want is to reduce the number of traversals.
;;;   For example, if we have "consequences are one" we want to roll it to one
;;; node, instead of two - instead of [consequences are] [are one] it should be
;;; [consequences are one].
;;;   We might also want to remove ones which have only one entrance and exit.

(def parseinfo {:arity 3 :end-strings #{"." "!" "?"} :delimiter-regex " "})
(def datamap (update-in (parse-counts test-text parseinfo) [:counts] #(into {} %)))
(def thead ["Ha" "!" "He"])

;;;   note - when you put it together, the :text won't fit nicely, you'll need
;;; to edit it on arity. Fix that!
(defn consolidate
  [{:keys [counts end] end :end} key]
    (loop [head key result head]
      (if (= (count (counts head)) 1)
        (let [next-head (ffirst (counts head))]
          (recur next-head (conj (vec result) (last next-head))))
        {:key key 
         :text result 
         :textlen (count (remove (into #{} end) result))
         :endloc (if-let [p (->> (into #{} end)
                                 (map #(.indexOf result %))
                                 (filter pos?)
                                 (seq))]
                   (apply min p)
                   -1)
         :edges (counts head)})))

#_(prn (consolidate datamap thead))

(defn consolidate-datamap
  [{counts-vec :counts :as datamap}]
  (let [counts (into {} counts-vec)
        singles (->> counts
                     #_(filter #(= 1 (count (second %))))
                     (map first))
        replacements (map #(consolidate datamap %) singles)]
    replacements))

#_(doall (map #(do #_(prn "Key: " (:key %))
                 #_(prn "Text: " (:text %))
                 (prn "Edges: " (:edges %))) 
            (consolidate-datamap datamap)))

(def cons-partial (consolidate-datamap datamap))
#_(prn (distinct (mapcat #(map first %) (map :edges cons-partial))))
;; These are the only edge nodes used, aside from the starts
(def used-edges (distinct (mapcat #(map first %) (map :edges cons-partial))))
#_(prn (concat (map first (:start datamap)) used-edges))

(def needed-keys (into #{} (concat (map first (:start datamap)) used-edges)))
(prn (filter #(needed-keys (:key %)) cons-partial))
(prn "keys required: " (count needed-keys))
(prn "keys used in current: " (count (:counts datamap)))
; aaand filter such that only current
;   Okay, hold on, before I go revamping everything - I should work out how 
; much this will actually help.