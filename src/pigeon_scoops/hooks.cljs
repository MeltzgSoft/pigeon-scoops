(ns pigeon-scoops.hooks
  (:require [uix.core :as uix :refer [defhook]]
            ["@auth0/auth0-react" :refer [useAuth0]]))

(defhook use-token []
         (let [{:keys [getAccessTokenSilently isAuthenticated]} (js->clj (useAuth0) :keywordize-keys true)
               [token set-token!] (uix/use-state nil)
               [loading? set-loading!] (uix/use-state true)]
           (uix/use-effect
             (fn []
               (set-loading! true)
               (if isAuthenticated
                 (-> (getAccessTokenSilently (clj->js {:authorizationParams {:audience "https://api.pigeon-scoops.com"
                                                                             :scope    "openid profile email offline_access"}}))
                     (.then (juxt set-token! (partial set-loading! false))))
                 (set-loading! false)))
             [getAccessTokenSilently isAuthenticated])
           {:token token
            :loading? loading?}))
