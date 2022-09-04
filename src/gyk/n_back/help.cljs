(ns gyk.n-back.help
  (:require [helix.dom :as d]))

(defn help-comp []
  (d/div
    (d/h4
      "Controls")
    (d/p
      (d/kbd "Space") " – Start / Stop")
    (d/p
      (d/kbd "Enter") " – Match")
    (d/br)
    (d/h4
      "Credits")
    (d/p
      "The design is inspired by "
      (d/a {:href "http://cognitivefun.net/test/4"} "cognitivefun.net/test/4")
      ".")))
