(ns pigeon-scoops.user-order.views
  (:require [pigeon-scoops.components.number-field :refer [number-field]]
            [pigeon-scoops.context :as ctx]
            [pigeon-scoops.recipe.context :as rctx]
            [pigeon-scoops.user-order.context :as octx]
            [reitit.frontend.easy :as rfe]
            [uix.core :as uix :refer [$ defui]]
            ["@mui/icons-material/Delete$default" :as DeleteIcon]
            ["@mui/icons-material/AddCircle$default" :as AddCircleIcon]
            ["@mui/material" :refer [Button
                                     FormControl
                                     InputLabel
                                     Select
                                     Stack
                                     IconButton
                                     List
                                     ListItemButton
                                     ListItemText
                                     MenuItem
                                     Paper
                                     TableContainer
                                     Table
                                     TableHead
                                     TableBody
                                     TableRow
                                     TableCell
                                     TextField
                                     Typography]]))

(defui order-list [{:keys [selected-order-id]}]
       (let [{:keys [orders new-order!]} (uix/use-context octx/orders-context)]
         ($ Stack {:direction "column"}
            ($ IconButton {:color    "primary"
                           :disabled (some keyword? (map :user-order/id orders))
                           :on-click #(rfe/push-state :pigeon-scoops.user-order.routes/order {:order-id (new-order!)})}
               ($ AddCircleIcon))

            ($ List {:sx (clj->js {:maxHeight "100vh"
                                   :overflow  "auto"})}
               (for [o orders]
                 ($ ListItemButton
                    {:key      (:user-order/id o)
                     :selected (= (:user-order/id o) selected-order-id)
                     :on-click #(rfe/push-state :pigeon-scoops.user-order.routes/order {:order-id (:user-order/id o)})}
                    ($ ListItemText {:primary (or (:user-order/note o) "[New Order]")})))))))


(defui order-item-row [{:keys [order-item]}]
       (let [{:constants/keys [unit-types order-statuses]} (uix/use-context ctx/constants-context)
             {:keys [recipes]} (uix/use-context rctx/recipes-context)
             {:keys [set-item! remove-item!]} (uix/use-context octx/order-context)]
         ($ TableRow
            ($ TableCell
               ($ FormControl
                  (let [full-recipes (if (some #(= (:recipe/id %) (:order-item/recipe-id order-item)) recipes)
                                       recipes
                                       (conj recipes {:recipe/id   (:order-item/recipe-id order-item)
                                                      :recipe/name (:recipe/name order-item)}))]
                    ($ Select {:value     (or (str (:order-item/recipe-id order-item)) "")
                               :on-change #(set-item! (assoc order-item :order-item/recipe-id (uuid (.. % -target -value))))}
                       (for [recipe (sort-by :recipe/name full-recipes)]
                         ($ MenuItem {:value (str (:recipe/id recipe)) :key (:recipe/id recipe)}
                            (:recipe/name recipe)))))))
            ($ TableCell
               ($ Stack {:direction "row" :spacing 1}
                  ($ number-field {:value          (:order-item/amount order-item)
                                   :set-value!     #(set-item! (assoc order-item :order-item/amount %))
                                   :hide-controls? true})
                  ($ FormControl
                     ($ Select {:value     (or (:order-item/amount-unit order-item) "")
                                :on-change #(set-item! (assoc order-item :order-item/amount-unit
                                                                         (->> unit-types
                                                                              (filter (fn [ut]
                                                                                        (= (name ut)
                                                                                           (.. % -target -value))))
                                                                              (first))))}
                        (for [ut unit-types]
                          ($ MenuItem {:value ut :key ut} (name ut)))))))
            ($ TableCell
               ($ FormControl
                  ($ Select {:value     (or (:order-item/status order-item) "")
                             :on-change #(set-item! (assoc order-item
                                                      :order-item/status
                                                      (keyword "status" (.. % -target -value))))}
                     (for [s order-statuses]
                       ($ MenuItem {:value s :key s} (name s))))))
            ($ TableCell
               ($ IconButton {:color    "error"
                              :on-click (partial remove-item! (:order-item/id order-item))}
                  ($ DeleteIcon))))))

(defui order-item-table []
       (let [{:keys [editable-order new-item!]} (uix/use-context octx/order-context)]
         ($ TableContainer {:component Paper}
            ($ Table
               ($ TableHead
                  ($ TableRow
                     ($ TableCell "Recipe")
                     ($ TableCell "Amount")
                     ($ TableCell "Status")
                     ($ TableCell
                        "Actions"
                        ($ IconButton {:color    "primary"
                                       :disabled (some keyword? (map :order-item/id (:user-order/items editable-order)))
                                       :on-click new-item!}
                           ($ AddCircleIcon)))))

               ($ TableBody
                  (for [i (:user-order/items editable-order)]
                    ($ order-item-row {:key (:order-item/id i) :order-item i})))))))

(defui order-control []
       (let [{:constants/keys [order-statuses]} (uix/use-context ctx/constants-context)
             {:keys [order editable-order set-editable-order! unsaved-changes? save!]} (uix/use-context octx/order-context)
             status-label-id (str "status-" (:user-order/status order))]

         (uix/use-effect
           (fn []
             (when order
               (set-editable-order! order)))
           [order set-editable-order!])

         ($ Stack {:direction "column" :spacing 1}
            ($ Stack {:direction "row"}
               ($ Button {:on-click #(rfe/push-state :pigeon-scoops.user-order.routes/orders)}
                  "Back to list")
               ($ Button {:disabled (not unsaved-changes?)
                          :on-click #(-> (save!)
                                         (.catch (fn [r]
                                                   (-> (.text r)
                                                       (.then js/alert)))))}
                  "Save")
               ($ Button {:on-click (partial set-editable-order! order)
                          :disabled (not unsaved-changes?)}
                  "Reset"))
            ($ TextField {:label     "Note"
                          :value     (or (:user-order/note editable-order) "")
                          :on-change #(set-editable-order! (assoc editable-order
                                                             :user-order/note (.. % -target -value)))})
            ($ FormControl
               ($ InputLabel {:id status-label-id} "Status")
               ($ Select {:label-id  status-label-id
                          :value     (or (:user-order/status editable-order) "")
                          :label     "Status"
                          :on-change #(set-editable-order! (assoc editable-order
                                                             :user-order/status (keyword "status" (.. % -target -value))))}
                  (for [s order-statuses]
                    ($ MenuItem {:value s :key s} (name s)))))
            ($ order-item-table))))

(defui order-view [{:keys [path]}]
       (let [{:keys [order-id]} path]
         ($ octx/with-order {:order-id order-id}
            ($ Stack {:direction "row" :spacing 1}
               ($ order-list {:selected-order-id order-id})
               ($ order-control)))))

(defui order-row [{:keys [order]}]
       (let [{:keys [delete!]} (uix/use-context octx/orders-context)]
         ($ TableRow
            ($ TableCell {:on-click #(rfe/push-state :pigeon-scoops.user-order.routes/order {:order-id (:user-order/id order)})}
               (or (:user-order/note order) "[New Order]"))
            ($ TableCell
               (when (:user-order/status order)
                 (name (:user-order/status order))))
            ($ TableCell
               ($ IconButton {:color    "error"
                              :on-click #(delete! (:user-order/id order))}
                  ($ DeleteIcon))))))


(defui orders-table []
       (let [{:keys [orders new-order!]} (uix/use-context octx/orders-context)]
         ($ TableContainer {:sx (clj->js {:maxHeight "calc(100vh - 75px)"
                                          :overflow  "auto"})}
            ($ Table {:sticky-header true}
               ($ TableHead
                  ($ TableRow
                     ($ TableCell "Note")
                     ($ TableCell "Status")
                     ($ TableCell
                        "Actions"
                        ($ IconButton {:color    "primary"
                                       :disabled (some keyword? (map :user-order/id orders))
                                       :on-click #(rfe/push-state :pigeon-scoops.user-order.routes/order {:order-id (new-order!)})}
                           ($ AddCircleIcon)))))
               ($ TableBody
                  (for [o orders]
                    ($ order-row {:key (:user-order/id o) :order o})))))))
