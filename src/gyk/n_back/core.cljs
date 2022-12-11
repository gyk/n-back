(ns gyk.n-back.core
  (:require [helix.core :refer [$ <>]]
            [helix.dom :as d]
            [helix.hooks]
            ["react-dom" :as rdom]
            ["react-transition-group" :refer [SwitchTransition CSSTransition]]
            ["react-bootstrap/Button" :as Button]
            ["react-bootstrap/Alert" :as Alert]
            ["react-bootstrap/Tabs" :as Tabs]
            ["react-bootstrap/Tab" :as Tab]
            ["react-bootstrap/Badge" :as Badge]
            [re-frame.core :as rf]
            [gyk.n-back.db :as db]
            [gyk.n-back.game :as game]
            [gyk.n-back.settings :refer [settings-comp]]
            [gyk.n-back.store :as store]
            [gyk.n-back.help :refer [help-comp]]
            [gyk.n-back.hooks :refer [<-sub]]
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


(rf/dispatch-sync [:initialize])

(defn game-panel
  []
  (let [game         (<-sub [:game])
        n            (<-sub [:n])
        started?     (<-sub [:started?])
        show-stat?   (<-sub [:show-stat?])
        round-number (<-sub [:round-number])
        can-signal?  (<-sub [:can-signal?])]
    ; Event handlers
    (letfn [(handle-start-stop []
              (rf/dispatch [:toggle-start-stop]))

            (handle-key-down [event]
              (case (.-key event)
                " " (handle-start-stop)
                "Enter" (rf/dispatch [:player-signal])
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
                (str "Trial #" round-number))
             ; The sliding card
             (card (game/current-item game) (game/round-number game))

             ; Gives the player some instant feedback
             (instant-result started? (:last-result game))

             ; Start/Stop
             (start-stop-button started? handle-start-stop)

             ; Player signals the match
             ($ Button {:onClick   #(rf/dispatch [:player-signal])
                        :className "cmd-btn"
                        :variant   "success"
                        :disabled  (not can-signal?)}
                " Match ")

             (let [show-enough-trials? (and (not show-stat?)
                                            (<= 10 (count (:history game)) 11))]
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

             (when show-stat?
               (d/div
                 (d/hr)
                 ; ----------------

                 ($ Alert {:show    show-stat?
                           :variant "info"}
                    ($ Alert/Heading
                       "Results")
                    (d/ul
                      {:style {:text-align "left"}}
                      (d/li "Correct = "
                        (util/percentage (game/correct-rate game)))
                      (d/li "Reaction time = "
                        (util/int-or-na (game/reaction-time game)) " ms (All) / "
                        (util/int-or-na (game/correct-reaction-time game)) " ms (Correct)\n")
                      (d/li "Combined time = "
                        (util/int-or-na (game/combined-time game)) " ms\n"))
                    (d/hr)
                    ($ Button {:onClick #(rf/dispatch [:hide-stat])
                               :variant "info"}
                       "Close"))))))))

(defn app []
  ; WORKAROUND: Disables "Settings" and "Help" tabs when the game is running, as
  ; react-transition-group causes wrong sliding card opacity when switching tabs.
  (let [is-running? (<-sub [:started?])]
    (<>
      (d/div
        {:class "main"}
        ($ Tabs {:defaultActiveKey "play"
                 :style            {:margin "0.5rem 0.5rem 1rem 0.5rem"}}
           ($ Tab {:title    "Play"
                   :eventKey "play"}
              (game-panel))
           ($ Tab {:title    "Settings"
                   :eventKey "settings"
                   :disabled is-running?}
              (d/div
                ($ settings-comp {:on-change-n        #(rf/dispatch [:set-local-store :n (str %)])
                                  :on-change-interval #(rf/dispatch [:set-local-store :interval (str %)])})))
           ($ Tab {:title    "Help"
                   :eventKey "help"
                   :disabled is-running?}
              ($ help-comp)))))))

(rdom/render ($ app) (.getElementById js/document "root"))
