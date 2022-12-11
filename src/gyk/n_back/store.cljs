(ns gyk.n-back.store
  (:require [re-frame.core :as rf]))

(def SETTING_KEY_PREFIX "n-back/settings/")

;; localStorage
(defn- set-item!
  [key val]
  (.setItem (.-localStorage js/window) key val))

(defn- get-item
  [key]
  (.getItem (.-localStorage js/window) key))

(defn- remove-item!
  [key]
  (.removeItem (.-localStorage js/window) key))


(rf/reg-cofx
  :get-local-store
  (fn [cofx key]
    (update cofx
            :local-store
            (fnil #(assoc % (keyword key) (get-item (str SETTING_KEY_PREFIX (name key))))
                  {}))))

(rf/reg-fx
  :set-local-store
  (fn [[key value]]
    (let [key (str SETTING_KEY_PREFIX (name key))]
      (set-item! key value))))

(rf/reg-event-fx
  :set-local-store
  (fn [_ [_ key value]]
    {:fx [[:set-local-store [key value]]]}))
