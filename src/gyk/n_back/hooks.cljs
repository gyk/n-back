(ns gyk.n-back.hooks
  "Wrapper around react hooks."
  (:require ["react" :as react]
            [re-frame.core :as rf]
            [goog.object]))

;; Ref: https://github.com/lilactown/hooks-demo/blob/master/src/hooks_demo/hooks.cljs

;; === State hook ===
(deftype StateHook [^:volatile-mutable value set-value]
  IEquiv
  (-equiv [o other] (identical? o other))

  IHash
  (-hash [o] (goog/getUid o))

  IDeref
  (-deref [_]
    value)

  IReset
  (-reset! [_ value']
    (set-value value')
    (set! value value')
    value')

  ISwap
  (-swap! [o f]
    (-reset! o (f (-deref o))))
  (-swap! [o f a]
    (-reset! o (f (-deref o) a)))
  (-swap! [o f a b]
    (-reset! o (f (-deref o) a b)))
  (-swap! [o f a b xs]
    (-reset! o (apply f (-deref o) a b xs)))

  IPrintWithWriter
  (-pr-writer [_ writer opts]
    (-write writer "#object [gyk.n-back.hooks.StateHook ")
    (pr-writer {:val value} writer opts)
    (-write writer "]")))

(defn use-state
  [initial]
  (let [[value set-value] (react/useState initial)]
    (StateHook. value set-value)))

(defn use-deref
  ([a]
   (use-deref a []))
  ([a deps]
   (let [[value set-value] (react/useState @a)]
     (react/useEffect
       (fn []
         (add-watch a :use-atom
                    (fn [_ _ _ v']
                      (set-value v')))
         #(do
            (remove-watch a :use-atom)))
       (clj->js deps))
     value)))

(defn use-sub
  [key]
  (use-deref (rf/subscribe key)))


(defn <-sub
  ([query]
   (<-sub query []))
  ([query deps]
   (let [r (react/useMemo
             #(rf/subscribe query)
             (clj->js deps))
         [v u] (react/useState @r)]
     (react/useEffect
       (fn []
         (let [t (reagent.core/track! #(u @r))]
           #(reagent.core/dispose! t)))
       (clj->js deps))
     v)))
