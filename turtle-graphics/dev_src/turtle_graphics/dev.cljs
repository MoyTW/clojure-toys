(ns turtle-graphics.dev
    (:require
     [turtle-graphics.core]
     [figwheel.client :as fw]))

(fw/start {
           :websocket-url "ws://localhost:4449/figwheel-ws"
           :build-id "dev"
           :on-jsload (fn []
                        ;; (stop-and-start-my app)
                        )})
