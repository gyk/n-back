(ns gyk.n-back.help)

(defn help-comp []
  [:div
   [:h4
    "Controls"]
   [:p
    [:kbd "Space"] " – Start / Stop"]
   [:p
    [:kbd "Enter"] " – Match"]
   [:br]
   [:h4
    "Credits"]
   [:p
    "The design is inspired by "
    [:a {:href "http://cognitivefun.net/test/4"} "cognitivefun.net/test/4"]
    "."]])
