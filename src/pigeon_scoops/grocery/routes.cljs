(ns pigeon-scoops.grocery.routes
  (:require [cljs.spec.alpha :as s]
            [pigeon-scoops.grocery.views :refer [groceries-table grocery-view]]))

(def routes ["/grocery"
             ["" {:name ::groceries
                  :view groceries-table}]
             ["/:grocery-id" {:name       ::grocery
                              :view       grocery-view
                              :parameters {:path {:grocery-id (s/or :uuid uuid? :key keyword?)}}}]])
