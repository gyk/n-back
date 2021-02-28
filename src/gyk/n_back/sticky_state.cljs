(ns gyk.n-back.sticky-state
  (:require [uix.core.alpha :as uix.core]))

;; localStorage
(defn- set-item!
  [key val]
  (.setItem (.-localStorage js/window) key val))

(defn- get-item
  [key]
  (.getItem (.-localStorage js/window) key))

#_(defn- remove-item!
  [key]
  (.removeItem (.-localStorage js/window) key))

;; Sticky state
; https://www.joshwcomeau.com/react/persisting-react-state-in-localstorage/

(defn sticky-state [key default-value convert]
  (let [value* (uix.core/state #(if-some [value-str (get-item key)]
                                  (convert value-str)
                                  default-value))]
    (uix.core/with-effect [key @value*]
      (set-item! key @value*)
      nil)
    value*))
