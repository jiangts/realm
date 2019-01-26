# realm

Implements the ["Elm architecture"](https://guide.elm-lang.org/architecture/) in
reagent.

Why realm? It's kind of a blend between reagent and elm. The library was also
written with a re-frame project in mind, so re-alm seemed to be in spirit.

## Usage

Define a model, events (named updates functions of type `model -> model`), and a
view function, and pass them into our `elm` function to create a realm
component. The point is that the model, events, and view are all local to the
realm component.

### Creating realm components

Follow the elm architecture, and define the following 3 things:
- `model`: the initial model state
- `events`: a map from keywords `k`, to values `v`, which are functions of type
  : `model -> model`) to dispatch updates to the model
- `view`: function returning a Form-1 or Form-3 component. DOES NOT return a
  Form-2 component function!
  - if you want to do this, reference a new Form-2 component within the view fn.

Create elm component by defining a new function and returning a call to `elm` as
a **function** (not as a component). Then, use the resulting component.


#### Example:
```clojure
(defn row [label input]
  [:div.row
   [:div.col-md-2 [:label label]]
   [:div.col-md-5 input]])

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
```
As shown in the example, the `view` function is passed the model, and two
functions `>dispatch` and `>set`. `>dispatch` takes the event keyword and any
number of args. `>set` is syntax sugar, and defined as `#(swap! model assoc-in
%1 %2)`.


### Using realm components
When you pass the `:model` prop into the resulting component, if the `:raf` key
in the externally passed
in model is greater than the `:raf` key in the local state, the local state will update.

When you pass the `:watch-model` prop into the resulting component, it will be
called each time the model changes with the value of the model.

#### Example:
```clojure
(defn parent-component []
  (let [model (r/atom {:weight 20
                       :height 100})
        _ (js/setTimeout #(swap! model assoc :height 30 :raf 1) 2000)]
    (fn [] [bmi-form {:model @model}])))
```

## Next

- enable specifying a validation fn for the model

## License

Copyright © 2019 Allan Jiang

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.

