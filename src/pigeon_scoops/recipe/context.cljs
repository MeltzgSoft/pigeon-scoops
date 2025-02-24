(ns pigeon-scoops.recipe.context
  (:require [pigeon-scoops.api :as api]
            [pigeon-scoops.hooks :refer [use-token]]
            [pigeon-scoops.utils :refer [determine-ops]]
            [reitit.frontend.easy :as rfe]
            [uix.core :as uix :refer [$ defui]]))

(def recipes-context (uix/create-context))
(def recipe-context (uix/create-context))

(defui with-recipes [{:keys [children]}]
       (let [{:keys [token]} (use-token)
             [recipes set-recipes!] (uix/use-state nil)
             [refresh? set-refresh!] (uix/use-state nil)
             refresh! #(set-refresh! (not refresh?))
             delete! (fn [recipe-id]
                       (-> (api/delete-recipe token recipe-id)
                           (.then refresh!)))]
         (uix/use-effect
           (fn []
             (-> (api/get-recipes token)
                 (.then vals)
                 (.then (partial apply concat))
                 (.then set-recipes!)))
           [token refresh?])
         ($ (.-Provider recipes-context) {:value {:recipes     recipes
                                                  :new-recipe! #(do
                                                                  (set-recipes! (conj recipes {:recipe/id :new}))
                                                                  :new)
                                                  :refresh!    refresh!
                                                  :delete!     delete!}}
            children)))

(defui with-recipe [{:keys [recipe-id scaled-amount scaled-amount-unit children]}]
       (let [{:keys [token loading?]} (use-token)
             refresh-recipes! (:refresh! (uix/use-context recipes-context))
             [recipe set-recipe!] (uix/use-state nil)
             [editable-recipe set-editable-recipe!] (uix/use-state nil)
             [refresh? set-refresh!] (uix/use-state nil)
             unsaved-changes? (not= recipe editable-recipe)
             set-ingredient! #(set-editable-recipe! (update editable-recipe
                                                            :recipe/ingredients
                                                            (fn [ingredients]
                                                              (map (fn [i]
                                                                     (if (= (:ingredient/id i)
                                                                            (:ingredient/id %)) % i))
                                                                   ingredients))))
             remove-ingredient! (fn [ingredient-id]
                                  (set-editable-recipe! (update editable-recipe
                                                                :recipe/ingredients
                                                                (partial
                                                                  remove
                                                                  #(= ingredient-id (:ingredient/id %))))))
             new-ingredient! (fn []
                               (set-editable-recipe! (update editable-recipe
                                                             :recipe/ingredients
                                                             #(conj % {:ingredient/id :new}))))
             save! (fn []
                     (let [unit-ops (determine-ops :ingredient/id
                                                   (:recipe/ingredients recipe)
                                                   (:recipe/ingredients editable-recipe))]
                       (-> (if (uuid? (:recipe/id editable-recipe))
                             (api/update-recipe token editable-recipe)
                             (-> (api/create-recipe token editable-recipe)
                                 (.then #(do (refresh-recipes!)
                                             (rfe/push-state :pigeon-scoops.recipe.routes/recipe
                                                             {:recipe-id (:id %)})))))
                           (.then (fn [_]
                                    (js/Promise.all (clj->js (concat
                                                               (map (partial api/create-ingredient token recipe-id) (:new unit-ops))
                                                               (map (partial api/update-ingredient token recipe-id) (:update unit-ops))
                                                               (map (partial api/delete-ingredient token recipe-id) (:delete unit-ops)))))))
                           (.then #(set-refresh! (not refresh?)))
                           (.catch (fn [r]
                                     (-> (.text r)
                                         (.then js/alert))
                                     (set-refresh! (not refresh?)))))))]
         (uix/use-effect
           (fn []
             (cond (keyword? recipe-id)
                   ((juxt set-recipe! set-editable-recipe!) {})
                   (and (not loading?) (some? recipe-id))
                   (-> (api/get-recipe token recipe-id (if (some? scaled-amount)
                                                         {:amount      scaled-amount
                                                          :amount-unit scaled-amount-unit}
                                                         {}))
                       (.then (juxt set-recipe! set-editable-recipe!))
                       (.catch #(do (js/alert "Could not load recipe")
                                    (set-editable-recipe! nil))))))
           [refresh? loading? token recipe-id scaled-amount scaled-amount-unit])
         ($ (.-Provider recipe-context) {:value {:recipe               recipe
                                                 :editable-recipe      editable-recipe
                                                 :scaled-amount        scaled-amount
                                                 :set-editable-recipe! set-editable-recipe!
                                                 :set-ingredient!      set-ingredient!
                                                 :remove-ingredient!   remove-ingredient!
                                                 :new-ingredient!      new-ingredient!
                                                 :unsaved-changes?     unsaved-changes?
                                                 :save!                save!}}
            children)))