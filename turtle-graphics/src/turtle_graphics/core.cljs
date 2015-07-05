(ns ^:figwheel-always turtle-graphics.core
    (:require [goog.dom :as dom]))

(enable-console-print!)

(println "Edits to this text should show up in your developer console.")

;; define your app data so that it doesn't get over-written on reload

(defn- new-turtle []
  {:x 150 :y 150 :heading 0 :pen? true})

(defonce app-state
  (atom {:canvas (.getElementById js/document "myCanvas")
         :context (.getContext (.getElementById js/document "myCanvas") "2d")
         :turtle (new-turtle)}))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)

(defn- reset-state []
  (let [canvas (dom/getElement "myCanvas")]
    (swap! app-state
           {:canvas canvas
            :context (.getContext canvas "2d")})))

(defn- context [] (:context @app-state))

;; TODO: Canvas width, height not hardcoded
(defn- clear-canvas []
  (.clearRect (context) 0 0 300 300))

;; #################################### MATH ###################################
;; Note that the trig operations in JS take radians as input.

(def ^:private pi (.-PI js/Math))

(defn deg->rad [r]
  (* r (/ pi 180)))

(defn sind [x]
  (.sin js/Math (deg->rad x)))

(defn cosd [x]
  (.cos js/Math (deg->rad x)))

(defn- rotate
  "Rotates clockwise [xd, yd] around [xo, yo], returns new [x y]."
  [xo yo xd yd heading]
  (let [xr (+ (* xd (cosd heading)) (* yd (sind heading)))
        yr (- (* yd (cosd heading)) (* xd (sind heading)))]
    [(+ xo xr) (+ yo yr)]))

;; ################################### TURTLE ##################################

(defn t-x [] (:x (:turtle @app-state)))
(defn t-y [] (:y (:turtle @app-state)))
(defn t-h [] (- 180 (:heading (:turtle @app-state))))
(defn t-p [] (:pen? (:turtle @app-state)))

(defn- update-turtle [k f]
  (swap! app-state update-in [:turtle k] f))

(defn- assoc-turtle [k v]
  (swap! app-state assoc-in [:turtle k] v))

;; Note that heading is transformed by 180
(defn turtle [] {:x (t-x) :y (t-y) :heading (t-h) :pen? (t-p)})

(defn- begin-path
  ([] (.beginPath (context)))
  ([[x y]] (begin-path x y))
  ([x y] (.beginPath (context)) (.moveTo (context) x y)))

(defn- move-to
  ([[x y]] (.moveTo (context) x y))
  ([x y] (.moveTo (context) x y)))

(defn- line-to
  ([[x y]] (.lineTo (context) x y))
  ([x y] (.lineTo (context) x y)))

(defn- close-stroke []
  (.closePath (context))
  (.stroke (context)))

(defn- draw-turtle []
  (let [{:keys [x y heading]} (turtle)]
    (begin-path (rotate x y 0 10 heading))
    (line-to (rotate x y -2 0 heading))
    (line-to (rotate x y 2 0 heading))
    (close-stroke)))

;; ################################## MOVEMENT #################################

(defn forward [n]
  (let [{:keys [x y heading pen?]} (turtle)
        [xn yn] (rotate x y 0 n heading)]
    (when pen?
      (begin-path x y)
      (line-to xn yn)
      (close-stroke))
    (assoc-turtle :x xn)
    (assoc-turtle :y yn)))

(defn back [n]
  (forward (- n)))

(defn right [n]
  (update-turtle :heading #(+ % n)))

(defn left [n]
  (right (- n)))

(defn set-position
  ([x y] (set-position x y (t-h)))
  ([x y h]
   (assoc-turtle :x x)
   (assoc-turtle :y y)
   (assoc-turtle :heading h)))

(defn reset-turtle []
  (swap! app-state assoc :turtle (new-turtle)))

(def fd forward)
(def bk back)
(def rt right)
(def lt left)

;; #################################### PEN ####################################

(defn pendown []
  (assoc-turtle :pen? true))

(defn penup []
  (assoc-turtle :pen? false))

(defn penstatus []
  (t-p))

(def pd pendown)
(def pu penup)
(def ps penstatus)

;; #################################### MAIN ###################################

(defn reset-canvas []
  (reset-turtle)
  (clear-canvas))

(defn- star-of-david []
  (left 30)

  (forward 50)
  (right 120)
  (forward 50)
  (right 120)
  (forward 50)
  (right 120)

  (right 30)  
  (penup)
  (forward 57)
  (pendown)
  
  (right 150)

  (forward 50)
  (right 120)
  (forward 50)
  (right 120)
  (forward 50)
  (right 120)

  (right 30)
  (penup)
  (forward 57)
  (pendown)
  (right 180))

(defn ^:export main []
  (reset-canvas)
  (star-of-david)
  (draw-turtle))

(main)
