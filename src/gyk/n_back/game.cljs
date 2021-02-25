(ns gyk.n-back.game
  (:require [amalloy.ring-buffer :refer [ring-buffer]]))

(defprotocol NBackGame
  (get-n [this])
  (current-item [this])
  (step [this ts])
  (signal [this ts])
  (round-number [this])
  (last-result [this]))

(defn can-match? [game]
  (if game
    (> (round-number game) (get-n game))
    false))

(def ^:private emoticons
  ["🍰" "🖊️" "🎾" "💓" "🐟" "🥄" "🚗" "🐱" "📚" "🥾"])

(defrecord Round
  [^String value
   ^number spawn-ts
   ^boolean is-matched?
   ^number signal-ts])

(defn- has-player-signaled? [^Round round]
  (some? (:signal-ts round)))

(defn- round-result [^Round round]
  (let [is-matched? (:is-matched? round)
        player-signaled? (has-player-signaled? round)
        delta-ts (when player-signaled?
                   (- (:signal-ts round) (:spawn-ts round)))]
    (case [is-matched? player-signaled?]
      [true true]
      [true delta-ts]

      [false true]
      [false delta-ts] ; records the reaction time anyway

      [true false]
      [false nil]

      ; [false false]
      nil)))

(defrecord EmoticonGame
  [n ; The N in N-back
   i ; The round number
   match-prob ; The probability of a match occurring
   queue ; The ring buffer of size n to store the recent items
   current-round
   last-result
   history])

(defn- append-history [history result]
  (if (some? result)
    (conj history result)
    history))

(extend-type EmoticonGame
  NBackGame
  (get-n [this]
    (:n this))

  (current-item [this]
    (-> this :current-round :value))

  (step [this ts]
    (let [current-round (:current-round this)
          current-item (:value current-round)
          player-signaled? (has-player-signaled? current-round)
          result' (when current-round
                    (if player-signaled?
                      (:last-result this) ; Has been computed, skipped.
                      (round-result current-round)))
          history' (if player-signaled?
                     (:history this) ; Has been appended to history
                     (append-history (:history this) result'))

          queue' (if current-item
                   (conj (:queue this) current-item)
                   (:queue this))
          can-match? (= (count queue') (:n this))
          should-match? (-> (and can-match?
                                 (< (rand) (:match-prob this)))
                            boolean)
          head (first queue')
          new-item (if should-match?
                     head
                     (->> (repeatedly #(rand-nth emoticons))
                          (filter #(not= % head))
                          (first)))
          new-round (Round. new-item ts should-match? nil)]
      (when-not (zero? ts)
        (println new-round))
      (assoc this
             :i (inc (:i this))
             :queue queue'
             :current-round new-round
             :last-result result'
             :history history')))

  (signal [this ts]
    (let [current-round (:current-round this)]
      (if (and (some? current-round)
               ; Do not update ts if the player signals multiple times.
               (not (has-player-signaled? current-round)))
        (let [current-round' (assoc current-round :signal-ts ts)
              result (round-result current-round')]
          (assoc this
                 :current-round current-round'
                 :last-result result
                 :history (append-history (:history this) result)))
        this)))

  (round-number [this]
    (:i this))

  (last-result [this]
    (:last-result this)))

; ts is needed in the case of 1-back.
(defn emoticon-game [n match-prob ts]
  {:pre [(pos-int? n) ; WTH `(pos? "2")` is true in CLJS
         (< 0.0 match-prob 1.0)
         (>= ts 0)]}
  (let [ts (or ts 0)
        q (ring-buffer n)
        game (map->EmoticonGame
              {:n n
               :i 0
               :match-prob match-prob
               :queue q
               :history []})]
    (step game ts)))

;; Time
;; ================

(defn reaction-time
  [this]
  (let [react-history (->> this
                           :history
                           (keep second))]
    (when-not (empty? react-history)
     (let [total (apply + react-history)]
       (/ total (count react-history))))))

(defn correct-reaction-time
  [this]
  (when-let [correct-history (->> this
                                  :history
                                  (filter first)
                                  not-empty)]
    (let [total (apply + (map second correct-history))]
      (/ total (count correct-history)))))

(defn correct-rate
  [this]
  (when-let [history (-> this :history not-empty)]
    (let [correct-history (filter first history)]
      (/ (count correct-history) (count history)))))

(defn combined-time
  [this]
  (let [crt (correct-reaction-time this)
        r (correct-rate this)]
    (when (and crt r)
      (/ crt r))))
