(ns gyk.n-back.sticky-state
  (:require [helix.hooks]
            [gyk.n-back.hooks :refer [use-state]]))

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
  (let [value* (use-state (if-some [value-str (get-item key)]
                            (convert value-str)
                            default-value))]
    (helix.hooks/use-effect [key @value*]
      (set-item! key @value*)
      nil)
    value*))
