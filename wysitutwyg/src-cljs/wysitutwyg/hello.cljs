(ns wysitutwyg.hello
  (:require [wysitutwyg.markov :as markov]))

(def sonnet-counts (markov/parse-counts 1
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

(defn create-sonnet []
  (do (js/alert (markov/generate-text ["Time"] 30 sonnet-counts))))
