(ns pigeon-scoops.user-order.context
  (:require [pigeon-scoops.api :as api]
            [pigeon-scoops.hooks :refer [use-token]]
            [pigeon-scoops.utils :refer [determine-ops]]
            [reitit.frontend.easy :as rfe]
            [uix.core :as uix :refer [$ defui]]))

(def orders-context (uix/create-context))
(def order-context (uix/create-context))

(defui with-orders [{:keys [children]}]
       (let [{:keys [token]} (use-token)
             [orders set-orders!] (uix/use-state nil)
             [refresh? set-refresh!] (uix/use-state nil)
             refresh! #(set-refresh! (not refresh?))
             delete! (fn [order-id]
                       (-> (api/delete-order token order-id)
                           (.then refresh!)))]
         (uix/use-effect
           (fn []
             (when token
               (-> (api/get-orders token)
                   (.then set-orders!))))
           [token refresh?])
         ($ (.-Provider orders-context) {:value {:orders     orders
                                                 :new-order! #(do
                                                                (set-orders! (conj orders {:user-order/id :new}))
                                                                :new)
                                                 :refresh!   refresh!
                                                 :delete!    delete!}}
            children)))

(defui with-order [{:keys [order-id children]}]
       (let [{:keys [token]} (use-token)
             refresh-orders! (:refresh (uix/use-context orders-context))
             [order set-order!] (uix/use-state nil)
             [editable-order set-editable-order!] (uix/use-state nil)
             [refresh? set-refresh!] (uix/use-state nil)
             unsaved-changes? (not= order editable-order)
             set-item! #(set-editable-order! (update editable-order
                                                     :user-order/items
                                                     (fn [items] (map (fn [i]
                                                                        (if (= (:order-item/id i)
                                                                               (:order-item/id %)) % i))
                                                                      items))))
             remove-item! (fn [item-id]
                            (set-editable-order! (update editable-order
                                                         :user-order/items
                                                         (partial
                                                           remove
                                                           #(= item-id (:order-item/id %))))))
             new-item! (fn []
                         (set-editable-order! (update editable-order
                                                      :user-order/items
                                                      #(conj % {:order-item/id :new}))))
             save! (fn []
                     (let [unit-ops (determine-ops :order-item/id
                                                   (:user-order/items order)
                                                   (:user-order/items editable-order))]
                       (-> (if (uuid? (:user-order/id editable-order))
                             (api/update-order token editable-order)
                             (-> (api/create-order token editable-order)
                                 (.then #(do (refresh-orders!)
                                             (rfe/push-state :pigeon-scoops.order.routes/order
                                                             {:order-id (:id %)})))))
                           (.then (fn [_]
                                    (js/Promise.all (clj->js (concat
                                                               (map (partial api/create-item token order-id) (:new unit-ops))
                                                               (map (partial api/update-item token order-id) (:update unit-ops))
                                                               (map (partial api/delete-item token order-id) (:delete unit-ops)))))))
                           (.then #(set-refresh! (not refresh?))))))]
         (uix/use-effect
           (fn []
             (cond (keyword? order-id)
                   ((juxt set-order! set-editable-order!) {})
                   (and order-id token)
                   (.then (api/get-order token order-id)
                          (juxt set-order! set-editable-order!))))
           [refresh? token order-id])
         ($ (.-Provider order-context) {:value {:order               order
                                                :editable-order      editable-order
                                                :set-editable-order! set-editable-order!
                                                :set-item!           set-item!
                                                :remove-item!        remove-item!
                                                :new-item!           new-item!
                                                :unsaved-changes?    unsaved-changes?
                                                :save!               save!}}
            children)))