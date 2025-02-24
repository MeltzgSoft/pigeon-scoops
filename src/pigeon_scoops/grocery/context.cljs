(ns pigeon-scoops.grocery.context
  (:require [pigeon-scoops.api :as api]
            [pigeon-scoops.hooks :refer [use-token]]
            [pigeon-scoops.utils :refer [determine-ops]]
            [reitit.frontend.easy :as rfe]
            [uix.core :as uix :refer [$ defui]]))

(def groceries-context (uix/create-context))
(def grocery-context (uix/create-context))

(defui with-groceries [{:keys [children]}]
       (let [{:keys [token]} (use-token)
             [groceries set-groceries!] (uix/use-state nil)
             [refresh? set-refresh!] (uix/use-state nil)
             refresh! #(set-refresh! (not refresh?))
             delete! (fn [grocery-id]
                       (-> (api/delete-grocery token grocery-id)
                           (.then refresh!)))]
         (uix/use-effect
           (fn []
             (when token
               (-> (api/get-groceries token)
                   (.then set-groceries!))))
           [token refresh?])
         ($ (.-Provider groceries-context) {:value {:groceries    groceries
                                                    :new-grocery! #(do
                                                                     (set-groceries! (conj groceries {:grocery/id :new}))
                                                                     :new)
                                                    :refresh!     refresh!
                                                    :delete!      delete!}}
            children)))

(defui with-grocery [{:keys [grocery-id children]}]
       (let [{:keys [token]} (use-token)
             refresh-groceries! (:refresh! (uix/use-context groceries-context))
             [grocery set-grocery!] (uix/use-state nil)
             [editable-grocery set-editable-grocery!] (uix/use-state nil)
             [refresh? set-refresh!] (uix/use-state nil)
             unsaved-changes? (not= grocery editable-grocery)
             set-unit! #(set-editable-grocery! (update editable-grocery
                                                       :grocery/units
                                                       (fn [units]
                                                         (map (fn [u]
                                                                (if (= (:grocery-unit/id u)
                                                                       (:grocery-unit/id %)) % u))
                                                              units))))
             remove-unit! (fn [unit-id]
                            (set-editable-grocery! (update editable-grocery
                                                           :grocery/units
                                                           (partial
                                                             remove
                                                             #(= unit-id (:grocery-unit/id %))))))
             new-unit! (fn []
                         (set-editable-grocery! (update editable-grocery
                                                        :grocery/units
                                                        #(conj % {:grocery-unit/id :new}))))
             save! (fn []
                     (let [unit-ops (determine-ops :grocery-unit/id
                                                   (:grocery/units grocery)
                                                   (:grocery/units editable-grocery))]
                       (-> (if (uuid? (:grocery/id editable-grocery))
                             (api/update-grocery token editable-grocery)
                             (-> (api/create-grocery token editable-grocery)
                                 (.then #(do (refresh-groceries!)
                                             (rfe/push-state :pigeon-scoops.grocery.routes/grocery
                                                             {:grocery-id (:id %)})))))
                           (.then (fn [_]
                                    (js/Promise.all (clj->js (concat
                                                               (map (partial api/create-grocery-unit token grocery-id) (:new unit-ops))
                                                               (map (partial api/update-grocery-unit token grocery-id) (:update unit-ops))
                                                               (map (partial api/delete-grocery-unit token grocery-id) (:delete unit-ops)))))))
                           (.then #(set-refresh! (not refresh?))))))]
         (uix/use-effect
           (fn []
             (cond
               (keyword? grocery-id)
               ((juxt set-grocery! set-editable-grocery!) {})
               (and grocery-id token)
               (.then (api/get-grocery token grocery-id) (juxt set-grocery! set-editable-grocery!))))
           [refresh? token grocery-id])
         ($ (.-Provider grocery-context) {:value {:grocery               grocery
                                                  :editable-grocery      editable-grocery
                                                  :set-editable-grocery! set-editable-grocery!
                                                  :set-unit!             set-unit!
                                                  :remove-unit!          remove-unit!
                                                  :new-unit!             new-unit!
                                                  :unsaved-changes?      unsaved-changes?
                                                  :save!                 save!}}
            children)))


