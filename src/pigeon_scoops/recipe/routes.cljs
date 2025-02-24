(ns pigeon-scoops.recipe.routes
  (:require [cljs.spec.alpha :as s]
            [pigeon-scoops.recipe.views :refer [recipe-view recipes-table]]
            [spec-tools.data-spec :as ds]))

(def routes
  ["/recipe"
   ["" {:name ::recipes
        :view recipes-table}]
   ["/:recipe-id" {:name       ::recipe
                   :view       recipe-view
                   :parameters {:path  {:recipe-id (s/or :uuid uuid? :key keyword?)}
                                :query {(ds/opt :amount)      number?
                                        (ds/opt :amount-unit) keyword?}}}]])
