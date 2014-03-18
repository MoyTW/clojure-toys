;; So, http://xkcd.com/1341/ discusses various types of editors. WYSIWYG, WYSINWYG, WYSITUTWYG, and WYSIHWD.
;; I figured I'd make a crack at a WYSITUTWYG editor. The plan is to use a markov chain and a seed to generate gibberish.

;; So, think about this for a moment. What do we need to do?

;; Generate a markov chain
;; Map user input to keys in the markov chain
;; Resolve the chain and return

;; Okay, let's get to generating.
;; First, counts.
;; Should be in the format of:
{"He" {"is" 2},
 "is" {"a" 2, "sad" 2}}

(def delimiters #{\space})
 
(defn not-delimiter? [char]
  (not (delimiters char)))
 
(defn snip-delimiters [coll]
  (drop-while delimiters coll))

(defn parse-counts [coll]
  (loop [counts {} corpus coll]
    (let [[word rest-corpus] (split-with not-delimiter? (snip-delimiters corpus))
          follows (take-while not-delimiter? (snip-delimiters rest-corpus))]
      (if (seq rest-corpus)
          (recur (update-in counts [word follows] (fnil inc 0)) rest-corpus)
          counts))))

(parse-counts "The quick brown fox.")
(parse-counts "The quick the quick brown fox.")

;; Now. Here's an important question. How do we handle things like ! or , or .? And how do we handle capitalization? Is the the same as The? Well, simply offing punctuation is not an option - there's important information encoded in it. It's isn't it is, actually.
;; Hmm. Well, we're not going for a Serious Thing here, so heck, we can just leave it.

(def ^:dynamic ^java.util.Random *rnd* (java.util.Random. 1))
(defn get-rand [x]
  (.nextInt *rnd* x))

(def counts (parse-counts 
"When I have seen by Time's fell hand defaced
The rich proud cost of outworn buried age;
When sometime lofty towers I see down-razed,
And brass eternal slave to mortal rage;
When I have seen the hungry ocean gain
Advantage on the kingdom of the shore,
And the firm soil win of the watery main,
Increasing store with loss, and loss with store;
When I have seen such interchange of state,
Or state itself confounded to decay;
Ruin hath taught me thus to ruminate
That Time will come and take my love away.
   This thought is as a death which cannot choose
   But weep to have that which it fears to lose.
For shame deny that thou bear'st love to any,
Who for thy self art so unprovident.
Grant, if thou wilt, thou art beloved of many,
But that thou none lov'st is most evident:
For thou art so possessed with murderous hate,
That 'gainst thy self thou stick'st not to conspire,
Seeking that beauteous roof to ruinate
Which to repair should be thy chief desire.
O! change thy thought, that I may change my mind:
Shall hate be fairer lodged than gentle love?
Be, as thy presence is, gracious and kind,
Or to thyself at least kind-hearted prove:
   Make thee another self for love of me,
   That beauty still may live in thine or thee."))

(defn pick-word [words]
  (if-let [word-seq (seq words)]
    (let [keys (vec (map first word-seq))
          counts (vec (map second word-seq))
          total-counts (reduce + counts)
          r (get-rand total-counts)]
      (loop [i 0 sum 0]
        (if (< r (+ (counts i) sum))
            (nth keys i)
            (recur (inc i) (+ (counts i) sum)))))))
;; That's a little convoluted. But basically, it generates a random from zero to total. Then, if it's less than the magnitude of the first connection, it chooses the first word. If it's not it adds that, then proceeds to the next connection and sums that.
;; So if you have 10, 15, and 5, what it does is:
;; Is random < 10? If so, return first word.
;; Is random < 10+15=25? If so, return second word.
;; Is random < 25+5=30? If so, return third word.
  
(defn chain [start-word counts]
  (loop [out [] word start-word]
    (if-let [next-word (pick-word (counts word))]
      (recur (conj out next-word) next-word)
      out)))
(clojure.string/join \space (map #(apply str %) (chain [\t \h \e] counts)))
(clojure.string/join \space (take 130 (map #(apply str %) (chain [\W \h \e \n] counts))))

(defn lazy-chain [start-word counts]
  (if-let [next-word (pick-word (counts start-word))]
    (cons start-word (lazy-seq (lazy-chain next-word counts)))))

(def sonnets-text (slurp "F:/aP-Personal_Projects/Clojure/wysitutwyg/resources/sonnets.txt"))
(def sonnets (parse-counts sonnets-text))
(clojure.string/join \space (take 130 (map #(apply str %) (lazy-chain [\W \h \e \n] sonnets))))