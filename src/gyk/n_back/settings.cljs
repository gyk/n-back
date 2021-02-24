(ns gyk.n-back.settings
  (:require ["react-bootstrap/Form" :as Form]))

(defn settings-comp
  [{:keys [n on-change-n
           interval on-change-interval]}]
  [:div
   [:> Form
    [:> Form/Group
     [:> Form/Label
      "N (as in " [:i "N"] "-back)"]
     [:> Form/Control {:as "select"
                       :default-value n
                       :on-change #(when on-change-n
                                     (on-change-n (.. % -target -value)))}
      (for [n (range 1 10)]
        [:option {:key n} n])]]

    [:> Form/Group
     [:> Form/Label
      "Interval (in millisecond)"]
     [:> Form/Control {:as "select"
                       :default-value interval
                       :on-change #(when on-change-interval
                                     (on-change-interval (.. % -target -value)))}
      (for [t (range 1500 (inc 4000) 500)]
        [:option {:key t} t])]]]])
