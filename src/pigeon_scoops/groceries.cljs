(ns pigeon-scoops.groceries
  (:require [clojure.string :as str]
            [pigeon-scoops.api :as api]
            [pigeon-scoops.components.number-field :refer [number-field]]
            [pigeon-scoops.context :as ctx]
            [pigeon-scoops.hooks :refer [use-token]]
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
                                     TextField]]))

(defui grocery-list [{:keys [selected-grocery-id]}]
       (let [{:keys [groceries new-grocery!]} (uix/use-context ctx/groceries-context)
             [filter-text set-filter-text!] (uix/use-state "")
             filtered-groceries (filter #(or (str/blank? filter-text)
                                             (str/includes? (str/lower-case (:grocery/name %))
                                                            (str/lower-case filter-text)))
                                        groceries)]
         ($ Stack {:direction "column"}
            ($ Stack {:direction "row"}
               ($ TextField {:label     "Filter"
                             :variant   "outlined"
                             :value     filter-text
                             :on-change #(set-filter-text! (.. % -target -value))})
               ($ IconButton {:color    "primary"
                              :disabled (some keyword? (map :grocery/id groceries))
                              :on-click #(rfe/push-state :pigeon-scoops.routes/grocery {:grocery-id (new-grocery!)})}
                  ($ AddCircleIcon)))
            ($ List {:sx (clj->js {:maxHeight "100vh"
                                   :overflow  "auto"})}
               (for [g (sort-by :grocery/name filtered-groceries)]
                 ($ ListItemButton
                    {:key      (:grocery/id g)
                     :selected (= (:grocery/id g) selected-grocery-id)
                     :on-click #(rfe/push-state :pigeon-scoops.routes/grocery {:grocery-id (:grocery/id g)})}
                    ($ ListItemText {:primary (or (:grocery/name g) "[New Grocery]")})))))))


(defui grocery-unit-row [{:keys [unit]}]
       (let [unit-types (update-keys (->> ctx/constants-context
                                          (uix/use-context)
                                          :constants/unit-types
                                          (group-by namespace))
                                     keyword)
             {:keys [set-unit! remove-unit!]} (uix/use-context ctx/grocery-context)]
         ($ TableRow
            ($ TableCell
               ($ TextField {:value     (:grocery-unit/source unit)
                             :on-change #(set-unit! (assoc unit :grocery-unit/source (.. % -target -value)))}))
            ($ TableCell
               ($ number-field {:value (:grocery-unit/unit-cost unit) :set-value! #(set-unit! (assoc unit :grocery-unit/unit-cost %))}))
            (for [[idx [value-key type-key option-key]]
                  (map-indexed vector [[:grocery-unit/unit-mass :grocery-unit/unit-mass-type :mass]
                                       [:grocery-unit/unit-volume :grocery-unit/unit-volume-type :volume]
                                       [:grocery-unit/unit-common :grocery-unit/unit-common-type :common]])]

              ($ TableCell {:key idx}
                 ($ Stack {:direction "row" :spacing 1}
                    ($ number-field {:value (value-key unit) :set-value! #(set-unit! (assoc unit value-key %))})
                    ($ FormControl
                       ($ Select {:value     (or (type-key unit) "")
                                  :on-change #(set-unit! (assoc unit type-key (keyword (name option-key) (.. % -target -value))))}
                          (for [o (option-key unit-types)]
                            ($ MenuItem {:value o :key o} (name o))))))))
            ($ TableCell
               ($ IconButton {:color    "error"
                              :on-click (partial remove-unit! (:grocery-unit/id unit))}
                  ($ DeleteIcon))))))



(defui grocery-unit-table []
       (let [{:keys [units new-unit!]} (uix/use-context ctx/grocery-context)]
         ($ TableContainer {:component Paper}
            ($ Table
               ($ TableHead
                  ($ TableRow
                     ($ TableCell "Source")
                     ($ TableCell "Cost")
                     ($ TableCell "Mass")
                     ($ TableCell "Volume")
                     ($ TableCell "Common")
                     ($ TableCell
                        "Actions"
                        ($ IconButton {:color    "primary"
                                       :disabled (some keyword? (map :grocery-unit/id units))
                                       :on-click new-unit!}
                           ($ AddCircleIcon)))))

               ($ TableBody
                  (for [u units]
                    ($ grocery-unit-row {:key (:grocery-unit/id u) :unit u})))))))

(defui grocery-control []
       (let [{:constants/keys [departments]} (uix/use-context ctx/constants-context)
             {:keys [grocery grocery-name set-name! department set-department! reset! unsaved-changes?]} (uix/use-context ctx/grocery-context)
             department-label-id (str "department-" (:grocery/id grocery))]

         (uix/use-effect
           (fn []
             (when grocery
               (reset! grocery)))
           [grocery reset!])

         ($ Stack {:direction "column" :spacing 1}
            ($ Stack {:direction "row"}
               ($ Button {:on-click #(rfe/push-state :pigeon-scoops.routes/groceries)}
                  "Back to list")
               ($ Button {:disabled (not unsaved-changes?)}
                  "Save")
               ($ Button {:on-click (partial reset! grocery)
                          :disabled (not unsaved-changes?)}
                  "Reset"))
            ($ TextField {:label     "Name"
                          :value     grocery-name
                          :on-change #(set-name! (.. % -target -value))})
            ($ FormControl
               ($ InputLabel {:id department-label-id} "Department")
               ($ Select {:label-id  department-label-id
                          :value     department
                          :label     "Department"
                          :on-change #(set-department! (keyword "department" (.. % -target -value)))}
                  (for [d departments]
                    ($ MenuItem {:value d :key d} (name d)))))
            ($ grocery-unit-table))))

(defui grocery-view [{:keys [path]}]
       (let [{:keys [grocery-id]} path]
         ($ ctx/with-grocery {:grocery-id grocery-id}
            ($ Stack {:direction "row" :spacing 1}
               ($ grocery-list {:selected-grocery-id grocery-id})
               ($ grocery-control)))))

(defui grocery-row [{:keys [grocery]}]
       ($ TableRow
          ($ TableCell {:on-click #(rfe/push-state :pigeon-scoops.routes/grocery {:grocery-id (:grocery/id grocery)})}
             (:grocery/name grocery))
          ($ TableCell
             (name (:grocery/department grocery)))
          ($ TableCell
             ($ IconButton {:color    "error"
                            :on-click #(prn "delete" (:grocery/id grocery))}
                ($ DeleteIcon)))))


(defui groceries-table []
       (let [{:keys [groceries]} (uix/use-context ctx/groceries-context)]
         ($ TableContainer {:sx (clj->js {:maxHeight "calc(100vh - 75px)"
                                          :overflow  "auto"})}
            ($ Table {:sticky-header true}
               ($ TableHead
                  ($ TableRow
                     ($ TableCell "Name")
                     ($ TableCell "Department")
                     ($ TableCell "Actions")))
               ($ TableBody
                  (for [g (sort-by :grocery/name groceries)]
                    ($ grocery-row {:key (:grocery/id g) :grocery g})))))))
