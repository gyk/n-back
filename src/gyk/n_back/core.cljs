(ns gyk.n-back.core
  (:require [helix.core :refer [$ <> defnc]]
            [helix.dom :as d]
            ["react-dom" :as rdom]
            ["react-transition-group" :refer [SwitchTransition CSSTransition]]
            ["react-bootstrap/Button" :as Button]
            ["react-bootstrap/Alert" :as Alert]
            ["react-bootstrap/Tabs" :as Tabs]
            ["react-bootstrap/Tab" :as Tab]
            ["react-bootstrap/Badge" :as Badge]
            [gyk.n-back.game :as game]
            [gyk.n-back.settings :refer [settings-comp]]
            [gyk.n-back.help :refer [help-comp]]
            [gyk.n-back.hooks :refer [use-state]]
            [gyk.n-back.sticky-state :refer [sticky-state]]
            [gyk.n-back.util :as util]))

(defn init []
  (set! (.-title js/document) "N-Back"))

;; Default settings
(def n 2)
(def interval-ms 2500)
(def match-probability 0.333)

(defn card [card-value i]
  (d/div
    ($ SwitchTransition {:mode "out-in"}
       ($ CSSTransition {:key        i
                         :classNames "fade"

                         :addEndListener
                         (fn [node done]
                           (.addEventListener node "transitionend" done false))}
          (d/div
            {:class "button-container"}
            ($ Button {:className "n-back-card"
                       :variant   "primary"}
               card-value))))))

(defn instant-result [started? result]
  (d/div
    (d/label
      {:class "instant-result"}
      (cond
        (not started?) "😼"
        (nil? result) "😺"
        (first result) "😻"
        :else "🙀"))))

(defn start-stop-button [started? on-change]
  ($ Button {:onClick     (fn []
                            (when on-change
                              (on-change started?)))
             :onMouseDown (fn [e]
                            (.preventDefault e))
             :className   "cmd-btn"
             :variant     "warning"}
     (if started?
       " Stop "
       " Start ")))

(defn- game-n [n] (game/emoticon-game n match-probability nil))
(defn- game-n-now [n] (game/emoticon-game n match-probability (util/get-current-ts)))

(def ^:private interval-id* (atom nil))

(defn game-panel [n interval on-change]
  (let [game*       (use-state #(game-n n))
        started?*   (use-state false)
        show-stat?* (use-state false)

        can-signal? #(and (game/can-match? @game*)
                          @started?*)]
    ; Event handlers
    (letfn [(handle-start-stop [started?]
              (let [started?' (not started?)]
                (if started?'
                  (do
                    (reset! game* (if (nil? (game/last-timestamp @game*))
                                    ; First time
                                    (game/with-timestamp @game* (util/get-current-ts))
                                    ; Restart
                                    (game-n-now n)))
                    (reset!
                      interval-id*
                      (js/setInterval
                        (fn []
                          (swap! game* game/step (util/get-current-ts)))
                        interval))
                    (reset! show-stat?* false))
                  (do
                    (js/clearInterval (-> (reset-vals! interval-id* nil)
                                          (first)))
                    (reset! show-stat?* true)))
                (reset! started?* started?')
                (when on-change
                  (on-change started?'))))

            (handle-player-signal []
              (when (can-signal?)
                (swap! game* game/signal (util/get-current-ts))))

            (handle-key-down [event]
              (case (.-key event)
                " " (handle-start-stop @started?*)
                "Enter" (handle-player-signal)
                ()))]
      ; Listens to keydown
      (helix.hooks/use-effect []
        (js/document.addEventListener "keydown" handle-key-down)
        #(js/document.removeEventListener "keydown" handle-key-down))

      ; UI
      (d/div {:class "game-panel"
              :style {:position "relative"}}
             ($ Badge {:variant "secondary"
                       :style   {:position "absolute"
                                 :display  "inline-block"
                                 :top      "-1rem"
                                 :left     "0.5rem"}}
                (str n "-back"))
             ($ Badge {:variant "light"
                       :pill    true
                       :style   {:position "absolute"
                                 :display  "inline-block"
                                 :top      "-1rem"
                                 :right    "0.5rem"}}
                (str "Trial #" (game/round-number @game*)))
             ; The sliding card
             (card (game/current-item @game*) (game/round-number @game*))


             ; Gives the player some instant feedback
             (instant-result @started?* (:last-result @game*))

             ; Start/Stop
             (start-stop-button @started?* (fn [started?]
                                             (handle-start-stop started?)))

             ; Player signals the match
             ($ Button {:onClick   handle-player-signal
                        :className "cmd-btn"
                        :variant   "success"
                        :disabled  (not (can-signal?))}
                " Match ")

             (let [show-stat?          @show-stat?*
                   show-enough-trials? (and (not show-stat?)
                                            (<= 10 (count (:history @game*)) 11))]
               (helix.hooks/use-effect [show-enough-trials? show-stat?]
                 (if (or show-enough-trials? show-stat?)
                   (util/scroll-to-bottom)
                   (util/scroll-to-top))
                 nil)

               (when show-enough-trials?
                 (d/div
                   (d/hr)
                   ; ----------------

                   ($ Alert {:variant "success"}
                      "Results now available (feel free to keep playing)"))))

             (when @show-stat?*
               (d/div
                 (d/hr)
                 ; ----------------

                 ($ Alert {:show    @show-stat?*
                           :variant "info"}
                    ($ Alert/Heading
                       "Results")
                    (d/ul
                      {:style {:text-align "left"}}
                      (d/li "Correct = "
                            (util/percentage (game/correct-rate @game*)))
                      (d/li "Reaction time = "
                            (util/int-or-na (game/reaction-time @game*)) " ms (All) / "
                            (util/int-or-na (game/correct-reaction-time @game*)) " ms (Correct)\n")
                      (d/li "Combined time = "
                            (util/int-or-na (game/combined-time @game*)) " ms\n"))
                    (d/hr)
                    ($ Button {:onClick #(reset! show-stat?* false)
                               :variant "info"}
                       "Close"))))))))

(defn app []
  (let [n*           (sticky-state "n-back/settings/n" n js/parseInt)
        interval*    (sticky-state "n-back/settings/interval" interval-ms js/parseInt)

        ; WORKAROUND: Disables "Settings" and "Help" tabs when the game is running, as
        ; react-transition-group causes wrong sliding card opacity when switching tabs.
        is-running?* (use-state false)]
    (<>
      (d/div
        {:class "main"}
        ($ Tabs {:defaultActiveKey "play"
                 :style            {:margin "0.5rem 0.5rem 1rem 0.5rem"}}
           ($ Tab {:title    "Play"
                   :eventKey "play"}
              (game-panel @n* @interval* #(reset! is-running?* %)))
           ($ Tab {:title    "Settings"
                   :eventKey "settings"
                   :disabled @is-running?*}
              (d/div
                ($ settings-comp {:n                  @n*
                                  :on-change-n        #(reset! n* %)
                                  :interval           @interval*
                                  :on-change-interval #(reset! interval* %)})))
           ($ Tab {:title    "Help"
                   :eventKey "help"
                   :disabled @is-running?*}
              ($ help-comp)))))))

(rdom/render ($ app) (.getElementById js/document "root"))
