(ns gyk.n-back.util
  (:require [goog.string :as gstring]
            [goog.string.format]))

(defn get-current-ts []
  (.now js/Date))

(def ^:private na "N/A")

(defn percentage ^String [^number r]
  (if r
    (gstring/format "%.1f%%" (* r 100.0))
    na))

(defn int-or-na ^String [^number t]
  (if t
    (str (int t))
    na))
