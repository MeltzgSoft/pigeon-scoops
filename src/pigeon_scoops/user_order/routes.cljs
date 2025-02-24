(ns pigeon-scoops.user-order.routes
  (:require [cljs.spec.alpha :as s]
            [pigeon-scoops.user-order.views :refer [order-view orders-table]]))

(def routes
  ["/order"
   ["" {:name ::orders
        :view orders-table}]
   ["/:order-id" {:name       ::order
                  :view       order-view
                  :parameters {:path {:order-id (s/or :uuid uuid? :key keyword?)}}}]])
