(ns pigeon-scoops.router
  (:require [pigeon-scoops.grocery.routes :as grocery]
            [pigeon-scoops.recipe.routes :as recipe]
            [pigeon-scoops.user-order.routes :as order]
            [reitit.coercion.spec :as rss]
            [reitit.frontend :as rf]
            [reitit.frontend.easy :as rfe]
            [uix.core :refer [$ defui] :as uix]))

(def router-context (uix/create-context))

(defui item [props]
       ($ :div (str (js->clj props :keywordize-keys true))))

(def routes
  [["/" {:name ::root
         :view item}]
   grocery/routes
   order/routes
   recipe/routes])

(defui with-router [{:keys [children]}]
       (let [router (uix/use-memo #(rf/router routes {:data {:coercion rss/coercion}}) [routes])
             [route set-route] (uix/use-state nil)]

         (uix/use-effect
           #(rfe/start! router set-route {:use-fragment false})
           [router])

         ($ (.-Provider router-context) {:value {:route route}}
            children)))
