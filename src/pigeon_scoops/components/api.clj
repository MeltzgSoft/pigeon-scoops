(ns pigeon-scoops.components.api
  (:require [clojure.tools.logging :as logger]
            [pigeon-scoops.components.config-manager :as cm]
            [pigeon-scoops.components.grocery-manager :as gm]
            [com.stuartsierra.component :as component]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [muuntaja.middleware :as mw]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.logger :as log-mw]
            [ring.middleware.defaults :as defaults]
            [ring.util.response :refer [response]]))

(defn get-groceries-handler [grocery-manager params]
  (fn [& _]
    (response (apply (partial gm/get-groceries grocery-manager) (:types params)))))

(defn app-routes [config-manager grocery-manager]
  (routes
    (GET "api/hello" {} (fn [_] (response "hello world")))
    (GET "api/v1/groceries" {params :params} (get-groceries-handler grocery-manager params))
    (route/not-found "Not Found")))

(defrecord Api [config-manager grocery-manager]
  component/Lifecycle

  (start [this]
    (let [{::cm/keys [app-host app-port]} (::cm/app-settings config-manager)]
      (logger/info (str "Starting server on host " app-host " port: " app-port))
      (assoc this :server (run-jetty
                            (-> (app-routes config-manager grocery-manager)
                                log-mw/wrap-with-logger
                                mw/wrap-format
                                (defaults/wrap-defaults {:params {:keywordize true
                                                                  :urlencoded true}}))
                            {:host  app-host
                             :port  app-port
                             :join? false}))))

  (stop [this]
    (logger/log :info "Stopping server")
    (.stop (:server this))
    (assoc this :server nil)))

(defn make-api []
  (map->Api {}))
