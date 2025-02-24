(ns pigeon-scoops.context
  (:require [pigeon-scoops.api :as api]
            [pigeon-scoops.hooks :refer [use-token]]
            [uix.core :as uix :refer [$ defui]]))

(def constants-context (uix/create-context))

(defui with-constants [{:keys [children]}]
       (let [[constants set-constants!] (uix/use-state nil)]
         (uix/use-effect
           (fn []
             (.then (api/get-constants) set-constants!))
           [])
         ($ (.-Provider constants-context) {:value constants}
            children)))
