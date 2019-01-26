(ns realm.core
  (:require [reagent.core :as r]))


(defn elm
  "Creates elm-style component.
  Define a model, events, and view function.
  The point is the model is local, but can be watched & updated externally

  Usage:
  Model: the initial model state
  Events: a map of k/v pairs (k is keyword, v is fn : model -> model) to dispatch updates to the model
  View: function returning a Form-1 or Form-3 component. DOES NOT return a Form-2 component function!
        if you want to do this, reference a new Form-2 component within the view fn.
  TODO enable specifying a validation fn for the model

  Create elm component by defining a new function and returning a call to `elm` as a FUNCTION
  (not as a component). Then, use the resulting component.

  The resulting component should be entirely insulated from knowledge of the outside the world.
  It has successfully lived life under a rock. It can now be used as a library.

  When you pass the :model prop into the resulting component, if the `:raf` key in the externally passed
  in model is greater than the `:raf` key in the local state, the local state will update.

  When you pass the :watch-model prop into the resulting component, it will be called each time the model
  changes with the value of the model"
  [{:keys [model events view]}]
  (let [model (r/atom (or model {}))
        handlers (volatile! events)
        >dispatch (fn [event & args]
                    (if-let [handler (get @handlers event)]
                      (reset! model (handler @model args))
                      (throw (js/Error. (str "Event handler '" event "' not found")))))
        >set #(swap! model assoc-in %1 %2)
        watching-model? (volatile! false)]
    (fn [props & children]
      ;; handle externally passed in state...
      (let [ext-model (:model props)
            ;; override / extend events
            override-events (:events props)
            watch-model (:watch-model props)]
        (when (or (not (contains? @model :raf))
                  (> (:raf ext-model) (:raf @model)))
          (when ext-model
            (reset! model ext-model))
          (when override-events
            (doseq [[ev handler] override-events]
              (when-let [orig-handler (get @handlers ev)]
                (vswap! handlers assoc ev (partial handler orig-handler))))))
        ;; handle model watcher
        (if watch-model
          (when (not @watching-model?)
            (add-watch model :watch (fn [_ _ oldval newval] (watch-model newval)))
            (vreset! watching-model? true))
          (when @watching-model?
            (remove-watch model :watch)
            (vreset! watching-model? false))))
      ;; pass elm machinery into view function
      (apply view (merge props {:model model
                                :>dispatch >dispatch
                                :>set >set}) children))))


(comment

  "More ideas:
  Send the component events (without tracking internal state)
  Create something like queries (akin to properties)

  another idea:
  https://github.com/jarohen/oak

  alexandergunnarson has a more complex idea here:
  https://github.com/Day8/re-frame/issues/264"



  (ns whatever.something
    (:require [re-frame.core          :as re]
              [reagent.core           :as reagent]
              [reagent.debug          :refer [dev?]]
              [reagent.impl.component :refer [react-class?]]
              [reagent.interop        :refer-macros [$ $!]]))

  (re/reg-event-db ::gc-local-state
                   (fn [db [_ ident]] (update db :local-state dissoc ident)))

  (defn with-local-state [component-f]
    (when (dev?) (assert (fn? component-f)))
    (-> (fn [& args]
          (let [ident         (cljs.core/random-uuid) ; `(gensym)` might be sufficient and faster but you get the point
                component-ret (apply component-f ident args)]
            (when (dev?) (assert (or (fn? component-ret) (react-class? component-ret))))
            (if (react-class? component-ret)
              (let [orig-component-will-unmount
                    (-> component-ret ($ :prototype) ($ :componentWillUnmount))]
                (doto component-ret
                  (-> ($ :prototype)
                    ($! :componentWillUnmount
                        (fn componentWillUnmount []
                          (this-as c
                                   (when-not (nil? orig-component-will-unmount)
                                     (.call orig-component-will-unmount c))
                                   (re/dispatch-sync [::gc-local-state ident])))))))
              (reagent/create-class
                {:render component-ret
                 :component-will-unmount
                 (fn [_] (re/dispatch-sync [::gc-local-state ident]))}))))
      (with-meta (meta component-f))
      (doto ($! :name (.-name component-f)))))
  ;;;; Usage:

  (re/reg-sub ::local-reactive-text
              (fn [db [_ ident]]
                (get-in db [:local-state ident :some-data])))

  (re/reg-event-db ::set-local-reactive-text
                   (fn [db [_ ident text]]
                     (assoc-in db [:local-state ident :some-data] text)))

  (def some-component
    (with-local-state
      (fn some-component [ident default-text]
        (let [reactive-text (re/subscribe [::local-reactive-text ident])]
          (fn []
            [:div {:on-mouse-up #(re/dispatch [::set-local-reactive-text ident "reactive text has been set"])}
             (or @reactive-text default-text)])))))

  (defn other-component []
    (fn []
      [[some-component "some default text 1"]
       [some-component "some default text 2"]]))






  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
  ;; FORMS ;;;;;;;;;;;;;;;;;;;;;
  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
  (defn row [label input]
    [:div.row
     [:div.col-md-2 [:label label]]
     [:div.col-md-5 input]])

  ;; elm function sugar
  (defn bmi-form []
    (elm {:view
          (fn [{:keys [model >dispatch >set]}]
            (let [bmi (let [{:keys [height weight]} @model]
                        (if (pos? (* height height))
                          (/ weight (* height height))
                          ""))]
              [:div
               [row "Height" [:input {:type "number" :id :height
                                      :value (:height @model)
                                      :on-change #(>set [:height] (-> % .-target .-value))}]]
               [row "Weight" [:input {:type "number" :id :weight
                                      :default-value (:weight @model)
                                      :on-change #(>set [:weight] (-> % .-target .-value))}]]
               [row "BMI" [:input {:type "number" :id :bmi :disabled true
                                   :value bmi}]]]))}))


;; unsugared elm reagent component
(defn bmi-form []
  (let [model (r/atom {})
        >update #(swap! model assoc-in %1 %2)]
    (fn [props]
      (let [ext-model (:model props)]
        (when (or (:raf @model true) (> (:raf ext-model) (:raf @model)))
          (reset! model ext-model)))
      ;; computed field
      (let [bmi (let [{:keys [height weight]} @model]
                  (if (pos? (* height height))
                    (/ weight (* height height))
                    ""))]
        [:div
         [row "Height" [:input {:type "number" :id :height
                                :value (:height @model)
                                :on-change #(>update [:height] (-> % .-target .-value))}]]
         [row "Weight" [:input {:type "number" :id :weight
                                :default-value (:weight @model)
                                :on-change #(>update [:weight] (-> % .-target .-value))}]]
         [row "BMI" [:input {:type "number" :id :bmi :disabled true
                             :value bmi}]]])))


  (defn bmi-form-fun []
    (let [model (r/atom {:weight 20
                         :height 100})
          _ (js/setTimeout #(swap! model assoc :height 30 :raf 1) 2000)]
      (fn [] [bmi-form {:model @model}])))
  ))
