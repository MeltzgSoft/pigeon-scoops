(ns pigeon-scoops.recipe.views
  (:require [clojure.string :as str]
            [pigeon-scoops.components.number-field :refer [number-field]]
            [pigeon-scoops.components.numbered-text-area :refer [numbered-text-area]]
            [pigeon-scoops.context :as ctx]
            [pigeon-scoops.grocery.context :as gctx]
            [pigeon-scoops.recipe.context :as rctx]
            [reitit.frontend.easy :as rfe]
            [uix.core :as uix :refer [$ defui]]
            ["@mui/icons-material/AddCircle$default" :as AddCircleIcon]
            ["@mui/icons-material/Delete$default" :as DeleteIcon]
            ["@mui/icons-material/CheckCircle$default" :as CheckCircleIcon]
            ["@mui/icons-material/Cancel$default" :as CancelIcon]
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
                                     Switch
                                     TableContainer
                                     Table
                                     TableHead
                                     TableBody
                                     TableRow
                                     TableCell
                                     TextField
                                     Typography]]))

(defui recipe-list [{:keys [selected-recipe-id]}]
       (let [{:keys [recipes new-recipe!]} (uix/use-context rctx/recipes-context)
             [filter-text set-filter-text!] (uix/use-state "")
             filtered-recipes (filter #(or (str/blank? filter-text)
                                           (str/includes? (str/lower-case (:recipe/name %))
                                                          (str/lower-case filter-text)))
                                      recipes)]
         ($ Stack {:direction "column"}
            ($ Stack {:direction "row"}
               ($ TextField {:label     "Filter"
                             :variant   "outlined"
                             :value     filter-text
                             :on-change #(set-filter-text! (.. % -target -value))})
               ($ IconButton {:color    "primary"
                              :disabled (some keyword? (map :recipe/id recipes))
                              :on-click #(rfe/push-state :pigeon-scoops.recipe.routes/recipe {:recipe-id (new-recipe!)})}
                  ($ AddCircleIcon)))
            ($ List {:sx (clj->js {:maxHeight "100vh"
                                   :overflow  "auto"})}
               (for [r (sort-by :recipe/name filtered-recipes)]
                 ($ ListItemButton
                    {:key      (:recipe/id r)
                     :selected (= (:recipe/id r) selected-recipe-id)
                     :on-click #(rfe/push-state :pigeon-scoops.recipe.routes/recipe {:recipe-id (:recipe/id r)})}
                    ($ ListItemText {:primary (or (:recipe/name r) "[New Recipe]")})))))))

(defui ingredient-row [{:keys [ingredient]}]
       (let [{:constants/keys [unit-types]} (uix/use-context ctx/constants-context)
             {:keys [set-ingredient! remove-ingredient!]} (uix/use-context rctx/recipe-context)
             {:keys [groceries]} (uix/use-context gctx/groceries-context)
             {:keys [recipes]} (uix/use-context rctx/recipes-context)
             [recipe-ingredient? set-recipe-ingredient!] (uix/use-state (some? (:ingredient/ingredient-recipe-id ingredient)))
             ingredient-label-id (str "ingredient-" (:ingredient/id ingredient))]

         (uix/use-effect
           (fn []
             (set-recipe-ingredient! (some? (:ingredient/ingredient-recipe-id ingredient))))
           [ingredient])

         ($ TableRow
            ($ TableCell
               ($ Stack {:direction "row" :spcing 1}
                  ($ Switch {:checked   recipe-ingredient?
                             :on-change #(set-recipe-ingredient! (.. % -target -checked))})
                  ($ Typography {:on-click #(apply rfe/push-state
                                                   (if recipe-ingredient?
                                                     [:pigeon-scoops.recipe.routes/recipe
                                                      {:recipe-id (:ingredient/ingredient-recipe-id ingredient)}
                                                      {:amount      (:ingredient/amount ingredient)
                                                       :amount-unit (:ingredient/amount-unit ingredient)}]
                                                     [:pigeon-scoops.grocery.routes/grocery
                                                      {:grocery-id (:ingredient/ingredient-grocery-id ingredient)}]))}
                     (if recipe-ingredient? "Recipe" "Grocery"))))
            ($ TableCell
               ($ FormControl
                  (if recipe-ingredient?
                    (let [full-recipes (if (some #(= (:recipe/id %) (:ingredient/ingredient-recipe-id ingredient)) recipes)
                                         recipes
                                         (conj recipes {:recipe/id   (:ingredient/ingredient-recipe-id ingredient)
                                                        :recipe/name (:recipe/name ingredient)}))]
                      ($ Select {:label-id  ingredient-label-id
                                 :value     (str (:ingredient/ingredient-recipe-id ingredient))
                                 :on-change #(set-ingredient! (-> ingredient
                                                                  (dissoc :ingredient/ingredient-grocery-id)
                                                                  (assoc :ingredient/ingredient-recipe-id (uuid (.. % -target -value)))))}
                         (for [recipe (sort-by :recipe/name full-recipes)]
                           ($ MenuItem {:value (str (:recipe/id recipe)) :key (:recipe/id recipe)}
                              (:recipe/name recipe)))))
                    (let [full-groceries (if (some #(= (:grocery/id %) (:ingredient/ingredient-grocery-id ingredient)) groceries)
                                           groceries
                                           (conj groceries {:grocery/id   (:ingredient/ingredient-grocery-id ingredient)
                                                            :grocery/name (:grocery/name ingredient)}))]
                      ($ Select {:label-id  ingredient-label-id
                                 :value     (str (:ingredient/ingredient-grocery-id ingredient))
                                 :on-change #(set-ingredient! (-> ingredient
                                                                  (dissoc :ingredient/ingredient-recipe-id)
                                                                  (assoc :ingredient/ingredient-grocery-id (uuid (.. % -target -value)))))}
                         (for [grocery (sort-by :grocery/name full-groceries)]
                           ($ MenuItem {:value (str (:grocery/id grocery)) :key (:grocery/id grocery)}
                              (:grocery/name grocery))))))))
            ($ TableCell
               ($ Stack {:direction "row" :spacing 1}
                  ($ number-field {:value          (:ingredient/amount ingredient)
                                   :set-value!     #(set-ingredient! (assoc ingredient :ingredient/amount %))
                                   :hide-controls? true})
                  ($ FormControl
                     ($ Select {:value     (or (:ingredient/amount-unit ingredient) "")
                                :on-change #(set-ingredient! (assoc ingredient :ingredient/amount-unit
                                                                               (->> unit-types
                                                                                    (filter (fn [ut]
                                                                                              (= (name ut)
                                                                                                 (.. % -target -value))))
                                                                                    (first))))}
                        (for [ut unit-types]
                          ($ MenuItem {:value ut :key ut} (name ut)))))))
            ($ TableCell
               ($ IconButton {:color    "error"
                              :on-click (partial remove-ingredient! (:ingredient/id ingredient))}
                  ($ DeleteIcon))))))

(defui ingredient-table []
       (let [{:keys [editable-recipe new-ingredient!]} (uix/use-context rctx/recipe-context)]
         ($ TableContainer {:component Paper}
            ($ Table
               ($ TableHead
                  ($ TableRow
                     ($ TableCell "Type")
                     ($ TableCell "Ingredient")
                     ($ TableCell "Amount")
                     ($ TableCell
                        "Actions"
                        ($ IconButton {:color    "primary"
                                       :disabled (some keyword? (map :ingredient/id (:recipe/ingredients editable-recipe)))
                                       :on-click new-ingredient!}
                           ($ AddCircleIcon)))))
               ($ TableBody
                  (for [i (:recipe/ingredients editable-recipe)]
                    ($ ingredient-row {:key (:ingredient/id i) :ingredient i})))))))

(defui recipe-control []
       (let [{:constants/keys [unit-types]} (uix/use-context ctx/constants-context)
             {:keys [recipe
                     editable-recipe set-editable-recipe!
                     scaled-amount
                     unsaved-changes?
                     save!]} (uix/use-context rctx/recipe-context)]

         (uix/use-effect
           (fn []
             (when recipe
               (set-editable-recipe! recipe)))
           [recipe set-editable-recipe!])

         ($ Stack {:direction "column" :spacing 1 :sx (clj->js {:width "100%"})}
            ($ Stack {:direction "row"}
               ($ Button {:on-click #(rfe/push-state :pigeon-scoops.recipe.routes/recipes)}
                  "Back to list")
               ($ Button {:disabled (or (some? scaled-amount) (not unsaved-changes?))
                          :on-click save!}
                  "Save")
               ($ Button {:on-click (partial set-editable-recipe! recipe)
                          :disabled (not unsaved-changes?)}
                  "Reset Changes")
               (when (some? scaled-amount)
                 ($ Button {:on-click #(rfe/push-state :pigeon-scoops.recipe.routes/recipe
                                                       {:recipe-id (:recipe/id recipe)})}
                    "Reset scaled amount")))
            ($ TextField {:label     "Name"
                          :value     (or (:recipe/name editable-recipe) "")
                          :on-change #(set-editable-recipe! (assoc editable-recipe :recipe/name (.. % -target -value)))})
            ($ Stack {:direction "row"}
               ($ Switch {:checked   (or (:recipe/public editable-recipe) false)
                          :on-change #(set-editable-recipe! (assoc editable-recipe :recipe/public (.. % -target -checked)))})
               ($ Typography (if (:recipe/public editable-recipe) "Public" "Private")))
            ($ TextField {:label     "Source"
                          :value     (or (:recipe/source editable-recipe) "")
                          :on-change #(set-editable-recipe! (assoc editable-recipe :recipe/source (.. % -target -value)))})
            ($ Stack {:direction "row" :spacing 1}
               ($ number-field {:value          (:recipe/amount editable-recipe)
                                :set-value!     #(set-editable-recipe! (assoc editable-recipe :recipe/amount %))
                                :label          "Amount"
                                :hide-controls? true})

               ($ FormControl
                  ($ Select {:value     (or (:recipe/amount-unit editable-recipe) "")
                             :label     "Unit"
                             :on-change #(set-editable-recipe! (assoc editable-recipe
                                                                 :recipe/amount-unit
                                                                 (->> unit-types
                                                                      (filter (fn [ut]
                                                                                (= (name ut)
                                                                                   (.. % -target -value))))
                                                                      (first))))}
                     (for [ut unit-types]
                       ($ MenuItem {:value ut :key ut} (name ut))))))
            ($ ingredient-table)
            ($ numbered-text-area {:lines      (:recipe/instructions editable-recipe)
                                   :set-lines! #(set-editable-recipe! (assoc editable-recipe :recipe/instructions %))}))))

(defui recipe-view [{:keys [path query]}]
       (let [{:keys [recipe-id]} path
             {:keys [amount amount-unit]} query]
         ($ rctx/with-recipe {:recipe-id recipe-id :scaled-amount amount :scaled-amount-unit amount-unit}
            ($ Stack {:direction "row" :spacing 1}
               ($ recipe-list {:selected-recipe-id recipe-id})
               ($ recipe-control)))))

(defui recipe-row [{:keys [recipe]}]
       (let [{:keys [delete!]} (uix/use-context rctx/recipes-context)]
         ($ TableRow
            ($ TableCell {:on-click #(rfe/push-state :pigeon-scoops.recipe.routes/recipe {:recipe-id (:recipe/id recipe)})}
               (or (:recipe/name recipe) "[New Recipe]"))
            ($ TableCell
               (if (:recipe/public recipe)
                 ($ CheckCircleIcon {:color "success"})
                 ($ CancelIcon {:color "error"})))
            ($ TableCell
               (when (and (:recipe/amount recipe) (:recipe/amount-unit recipe))
                 (str (:recipe/amount recipe) " " (name (:recipe/amount-unit recipe)))))
            ($ TableCell
               ($ IconButton {:color    "error"
                              :on-click #(delete! (:recipe/id recipe))}
                  ($ DeleteIcon))))))

(defui recipes-table []
       (let [{:keys [recipes new-recipe!]} (uix/use-context rctx/recipes-context)]
         ($ TableContainer {:sx (clj->js {:maxHeight "calc(100vh - 75px)"
                                          :overflow  "auto"})}
            ($ Table {:sticky-header true}
               ($ TableHead
                  ($ TableRow
                     ($ TableCell "Name")
                     ($ TableCell "Public")
                     ($ TableCell "Amount")
                     ($ TableCell
                        "Actions"
                        ($ IconButton {:color    "primary"
                                       :disabled (some keyword? (map :recipe/id recipes))
                                       :on-click #(rfe/push-state :pigeon-scoops.recipe.routes/recipe {:recipe-id (new-recipe!)})}
                           ($ AddCircleIcon)))))
               ($ TableBody
                  (for [r (sort-by :recipe/name recipes)]
                    ($ recipe-row {:key (:recipe/id r) :recipe r})))))))
