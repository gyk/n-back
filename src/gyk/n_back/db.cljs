(ns gyk.n-back.db
  (:require [gyk.n-back.game :as game]
            [gyk.n-back.util :as util]
            [re-frame.core :as rf]))

;; Default settings

(def match-probability 0.333)

(def default-settings
  {:n        2
   :interval 2500})

(defn load-stored-settings
  [local-store]
  (let [{:keys [n interval]} local-store]
    {:n        (when n (js/parseInt n))
     :interval (when interval (js/parseInt interval))}))

(defn merge-settings-with
  [new-settings]
  (into default-settings
        (remove (comp nil? val))
        new-settings))

(defn- game-n [n] (game/emoticon-game n match-probability nil))
(defn- game-n-now [n now-ts] (game/emoticon-game n match-probability now-ts))

(defn default-db
  [settings]
  {:game        (game-n (:n settings))
   :started?    false
   :show-stat?  false
   :can-signal? false
   :interval-ms (:interval settings)
   :interval-id nil})

;; Subscriptions

(rf/reg-sub
  :game
  :-> :game)

(rf/reg-sub
  :started?
  :-> :started?)

(rf/reg-sub
  :show-stat?
  :-> :show-stat?)

(rf/reg-sub
  :interval-ms
  :-> :interval-ms)

(rf/reg-sub
  :interval-id
  :-> :interval-id)

(rf/reg-sub
  :n
  (fn [db _]
    (game/get-n (:game db))))

(rf/reg-sub
  :round-number
  (fn [db _]
    (game/round-number (:game db))))

(rf/reg-sub
  :current-item
  (fn [db _]
    (game/current-item (:game db))))

(rf/reg-sub
  :last-result
  (fn [db _]
    (game/last-result (:game db))))

(defn- can-signal?
  [{:keys [game started?]}]
  (and (game/can-match? game)
       started?))

(rf/reg-sub
  :can-signal?
  (fn [db _]
    (can-signal? db)))

;; Event Handlers

(rf/reg-event-fx
  :initialize
  [(rf/inject-cofx :get-local-store :n)
   (rf/inject-cofx :get-local-store :interval)]
  (fn [cofx _]
    (let [stored-settings (load-stored-settings (:local-store cofx))
          settings        (merge-settings-with stored-settings)]
      {:db (default-db settings)})))

(rf/reg-event-fx
  :start-game
  [(rf/inject-cofx :now)
   (rf/inject-cofx :get-local-store :n)
   (rf/inject-cofx :get-local-store :interval)]
  (fn [{:keys [db now local-store]}]
    (let [stored-settings (load-stored-settings local-store)
          settings        (merge-settings-with stored-settings)
          {:keys [n interval]} settings]
      {:db (-> db
               (assoc :started? true
                      :show-stat? false)
               (update :game
                       (fn [game]
                         (let [reuse-current? (and (nil? (game/last-timestamp game))
                                                   (= (game/get-n game) n))]
                           (if reuse-current?
                             ; First time
                             (game/with-timestamp game now)
                             ; Restart
                             (game-n-now n now))))))
       :fx [[:dispatch [:start-timer interval]]]})))

(rf/reg-event-fx
  :stop-game
  (fn [{:keys [db]} _]
    {:db (assoc db :started? false
                   :show-stat? true)
     :fx [[:stop-timer (:interval-id db)]]}))

(rf/reg-event-fx
  :toggle-start-stop
  (fn [{:keys [db]} _]
    (let [{:keys [started?]} db
          started?' (not started?)
          event     (if started?'
                      [:start-game]
                      [:stop-game])]
      {:db db
       :fx [[:dispatch event]]})))

(rf/reg-event-fx
  :step-game
  [(rf/inject-cofx :now)]
  (fn [{:keys [db now]} _]
    {:db (update db :game game/step now)}))

(rf/reg-event-fx
  :player-signal
  [(rf/inject-cofx :now)]
  (fn [{:keys [db now]} _]
    (if (can-signal? db)
      {:db (update db :game game/signal now)}
      {:db db})))

(rf/reg-event-fx
  :start-timer
  (fn [cofx [_ interval-ms]]
    (let [db          (:db cofx)
          interval-id (js/setInterval
                        (fn []
                          (rf/dispatch [:step-game]))
                        interval-ms)]
      {:db (assoc db :interval-id interval-id)})))

(rf/reg-fx
  :stop-timer
  (fn [interval-id]
    (js/clearInterval interval-id)))

(rf/reg-event-db
  :hide-stat
  (fn [db _]
    (assoc db :show-stat? false)))

(rf/reg-cofx
  :now
  (fn [cofx]
    (assoc cofx :now (util/get-current-ts))))
