(ns wysitutwyg.markov.parser)

(def test-text "Me is mad! It is mad? She is mad. He is sad! It is sad? She is sad. Ze mad? Ze sad? Ze jelly? Ha! He is mad! He is mad!")

;; I won't be winning any elegant code competitions...
(defn split-by-string
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

(defn split-step
  [out in]
  (mapcat #(split-by-string % in) out))
  
(defn split-by-all
  [string splitters]
  (reduce split-step (split-by-string string (first splitters)) (rest splitters)))

(defn process-corpus [corpus {:keys [delimiter-regex end-strings]}]
  "Processes the corpus into a collection of segments, determined by the
  delimiter set. Members of end-strings are grouped into their own segments."  
  (->> (.split corpus delimiter-regex)
       (vec) ; If there are no splits, String.split produces [corpus].
       (map #(apply str %))
       (mapcat #(split-by-all % end-strings))))

(defn keys-to-strings [s]
  (vec (map #(apply str %) s)))

(defn inner-map [m]
  (into {} (map (fn [[k v]] [(apply str k) v]) m)))

(defn stringify
  [counts]
  (->> counts
       (map #(update-in % [0] keys-to-strings))
       (map #(update-in % [1] inner-map))))

(defn update-start
  "Updates iff the arity of the key is equal to n."
  [start n follows rest-corpus]
  ;TODO: ugly
  (if (= n 1)
      (update-in start [[follows]] (fnil inc 0))
      (let [key [(apply conj [follows] (take (dec n) (rest rest-corpus)))]] 
        (if (= n (apply count key))
            (update-in start key (fnil inc 0))
            start))))

(defn build-output-map
  [start counts end-strings]
  {:start (map #(update-in % [0] keys-to-strings) start)
   :end (map str end-strings)
   :counts (stringify counts)})

(defn parse-step
  "Recursive step producing a map of the form 
  {:start {STATE0 COUNT0, STATE1 COUNT1, ... STATEn COUNTn}
   :end (ENDSTR0, ENDSTR1 ... ENDSTRn)
   :counts {STATE0 NEXT0, STATE1 NEXT1, ... STATEn NEXTn}}
  where STATEn is an array of strings
  COUNTn is an int
  NEXTn := [STATEn {STR0 COUNT0, STRn, COUNTn}]
  This isn't really good documentation; it'll confuse people who aren't me."
  [corpus n end-strings]
  (loop [counts {} 
         start (update-start {} n (first corpus) corpus)
         corpus corpus]
    (let [[state rest-corpus] (split-at n corpus)
           follows (first rest-corpus)]
      (cond
        (and (seq rest-corpus) (end-strings (last state)))
          (recur (update-in counts [state follows] (fnil inc 0))
                 (update-start start n follows rest-corpus)
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

(prn (parse-counts test-text {:arity 1 :end-strings #{"." "!" "?"} :delimiter-regex " "}))

(def expected-output
{:start '([["Ha"] 1] [["Ze"] 3] [["He"] 3] [["She"] 2] [["It"] 2] [["Me"] 1]), :end '("!" "." "?"), :counts '([["!"] {"He" 2, "It" 2}] [["is"] {"sad" 3, "mad" 5}] [["It"] {"is" 2}] [["Ze"] {"jelly" 1, "sad" 1, "mad" 1}] [["."] {"Ze" 1, "He" 1}] [["She"] {"is" 2}] [["mad"] {"." 1, "?" 2, "!" 3}] [["sad"] {"." 1, "?" 2, "!" 1}] [["Me"] {"is" 1}] [["Ha"] {"!" 1}] [["He"] {"is" 3}] [["jelly"] {"?" 1}] [["?"] {"Ha" 1, "Ze" 2, "She" 2}])})

(assert (= expected-output (parse-counts test-text {:arity 1 :end-strings #{"." "!" "?"} :delimiter-regex " "})))