(ns gyk.n-back.core
  (:require [uix.core.alpha :as uix.core]
            [uix.dom.alpha :as uix.dom]
            ["react-transition-group" :refer [SwitchTransition CSSTransition]]
            ["react-bootstrap/Button" :as Button]
            ["react-bootstrap/Alert" :as Alert]
            [gyk.n-back.game :as game]
            [gyk.n-back.util :as util]))

(defn init []
  (set! (.-title js/document) "N-Back"))

;; Config
(def n 2)
(def match-probability 0.333)

(defn card [card-value i]
  [:<>
   [:div
    [:> SwitchTransition {:mode "out-in"}
     [:> CSSTransition {:key i
                        :class-names "fade"

                        :add-end-listener
                        (fn [node done]
                          (.addEventListener node "transitionend" done false))}
      [:div.button-container
       [:> Button {:class-name "n-back-card"
                   :variant "primary"}
        card-value]]]]]])

(defn instant-result [started? result]
  [:div
   [:label {:class-name "instant-result"}
    (cond
      (not started?) "😼"
      (nil? result)  "😺"
      (first result) "😻"
      :else          "🙀")]])

(defn start-stop-button [started? on-change]
  [:> Button {:on-click (fn []
                          (when on-change
                            (on-change started?)))
              :class-name "cmd-btn"
              :variant "warning"}
   (if started?
     " Stop "
     " Start ")])

(def game (game/emoticon-game n match-probability nil))
(defn- game-now [] (game/step game (util/get-current-ts)))

(def ^:private interval-id* (atom nil))

(defn app []
  (let [game*        (uix.core/state game)
        last-result* (uix.core/cursor-in game* [:last-result])
        started?*    (uix.core/state false)
        show-stat?*  (uix.core/state false)]
    [:<>
     [:div.main
      [:div.main-inner
       ; The sliding card
       [card (game/current-item @game*) (game/round-number @game*)]

       ; Gives the player some instant feedback
       [instant-result @started?* @last-result*]

       ; Start/Stop
       [start-stop-button
        @started?*
        (fn [started?]
          (let [started?' (not started?)]
            (if started?'
              (do
                (reset! game* (game-now))
                (reset!
                 interval-id*
                 (js/setInterval
                  (fn []
                    (swap! game* game/step (util/get-current-ts)))
                  2500))
                (reset! show-stat?* false))
              (do
                (js/clearInterval (-> (reset-vals! interval-id* nil)
                                      (first)))
                (reset! show-stat?* true)))
            (reset! started?* started?')))]

       ; Player signals the match
       [:> Button {:on-click (fn []
                               (swap! game* game/signal (util/get-current-ts)))
                   :class-name "cmd-btn"
                   :variant "success"
                   :disabled (not (and (game/can-match? @game*)
                                       @started?*))}
        " Match "]

       [:hr] ; ----------------

       (when @show-stat?*
         [:> Alert {:show @show-stat?*
                    :variant "info"}
          [:> Alert/Heading
           "Game Result"]
          [:ul {:style {:text-align "left"}}
           [:li "Correct = "
            (util/percentage (game/correct-rate @game*))]
           [:li "Reaction time = "
            (util/int-or-na (game/reaction-time @game*)) " ms (All) / "
            (util/int-or-na (game/correct-reaction-time @game*)) " ms (Correct)\n"]
           [:li "Combined time = "
            (util/int-or-na (game/combined-time @game*)) " ms\n"]]
          [:hr]
          [:> Button {:on-click #(reset! show-stat?* false)
                      :variant "info"}
           "Close"]])]]]))

(uix.dom/render [app] (.getElementById js/document "root"))
