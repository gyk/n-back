(ns gyk.n-back.settings
  (:require [helix.core :refer [$ defnc]]
            [helix.dom :as d]
            [gyk.n-back.hooks :refer [<-sub]]
            ["react-bootstrap/Form" :as Form]))

(defnc settings-comp
  [{:keys [on-change-n on-change-interval]}]
  (let [n        (<-sub [:n])
        interval (<-sub [:interval-ms])]
    (d/div
      ($ Form
         ($ Form/Group
            ($ Form/Label
               "N (as in " (d/i "N") "-back)")
            ($ Form/Control {:as           "select"
                             :defaultValue n
                             :onChange     #(when on-change-n
                                              (on-change-n (js/parseInt (.. % -target -value))))}
               (for [n (range 1 10)]
                 (d/option {:key n} n))))

         ($ Form/Group
            ($ Form/Label
               "Interval (in milliseconds)")
            ($ Form/Control {:as           "select"
                             :defaultValue interval
                             :onChange     #(when on-change-interval
                                              (on-change-interval (js/parseInt (.. % -target -value))))}
               (for [t (range 1500 (inc 4000) 500)]
                 (d/option {:key t} t))))))))
