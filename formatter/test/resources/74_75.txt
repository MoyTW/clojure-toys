;;; 74-75 - In Which I Do Not Really Change The Underlying Algorithms

;;;; 74 - Filter Perfect Squares
;; Original:
(fn to-union [s]
  (let [nums (apply conj (sorted-set) 
                         (map #(Integer. %) 
                              (clojure.string/split s #",")))]
    (reduce #(str %1 "," %2)
            (clojure.set/intersection
              (apply conj
                     (sorted-set)
                     (rest 
                       (take (inc (Math/ceil (Math/sqrt (apply max nums))))
                             (map #(* % %) (range)))))
              nums))))
;; There are two parts to this one. First, you have to convert to and from the, uh, comma-separated integer strings and second, you have to detect which of the two are perfet squares.
;; Really, the comma-separated strings? Strikes me as kind of silly. Why would you ever...I guess if you have to read or write a .csv file, but then you really shouldn't be rolling your own .csv code. It seems deceptively simple but then somebody tosses some really weird data in and it breaks and I'm getting off-topic.
;; Anyways, what's going on here? Well, first, in the let we bind nums to a set of integers (sorted, because while there's nothing that technically says that the input goes out sorted, one might note that the unit tests only test in ascending order).
;; Then, we generate all the primes up to the maximum value in the string (inclusive), and take the intersection of the two sets, leaving us with all primes in the list.
;; Finally, we reduce over the list of numbers, inserting a comma between them and turning it them into a string.
;; My formatting is kind of confusing, reading it again, but I haven't figured out how to do (rest (take (long form) (long form))) without it looking wonky; either you put the rest and the take on the same line, which extends the line by an absurd amount, or you newline the take and end up with the slope, which isn't great either!
;; All right, so how might we rewrite this? Well, first off, we can actually just use join, instead of reducing. We can use take-while to simplify the generation of the set of primes. Also, that (apply conj (sorted set) (...)) could be (into (sorted-set) (...)).
(fn to-union [s]
  (let [nums (into (sorted-set) (map #(Integer. %) 
                                     (clojure.string/split s #",")))]
    (clojure.string/join ","
            (clojure.set/intersection 
              (into #{} (take-while #(<= % (apply max nums)) 
                                    (map #(* % %) (range))))
              nums))))
;; I guess we could use filter instead of intersection, if you're into that sort of thing, but I don't really get a whole lot of use out of intersection, so I think I'll stick with it here. Let's step back and examine the algorithm.
;; Efficiency-wise, it's prettey decent - it's not doing a ton that it doesn't have to and it's linear according to the maximum value in the string. I don't see any obvious difficulties here. I proclaim it sound.

;;;; 75 - Euler's Totient Function
;; Original:
(fn toitent [n]
  (let [gcd (fn [a b] 
              (loop [a a b b] 
                (if (= a b) a
                  (if (> a b) 
                    (recur (- a b) b) 
                    (recur a (- b a))))))]
    (count (filter #(= (gcd % n) 1) 
                   (rest (take (inc n) (range)))))))
;; For a description of the maths, see http://en.wikipedia.org/wiki/Euler%27s_totient_function. It's basically the number of coprimes which are less than that integer. The easiest way to calculate it is to simply find all the numbers with whom the integer shares a gcd equal to one, and count them (or, conversely, find all the numbers for which it has a gcd greater than one and subtract that number). So, that's what's happening here!
;; The gcd code was stolen from the earlier #66, and so I won't cover it here. You can go back and examine my earlier comments, if you wish.
;; The rest of the algorithm is, frankly, pretty boring. It just generates the integers from 1 to (n + 1), and frankly I don't know why I'm still monkeying around with the (rest (take)) business when range has a perfectly good pair of start and end parameters!
(fn toitent [n]
  (let [gcd (fn [a b] 
              (loop [a a b b] 
                (if (= a b) a
                  (if (> a b) 
                    (recur (- a b) b) 
                    (recur a (- b a))))))]
    (count (filter #(= (gcd % n) 1) 
                   (range 1 (inc n))))))