(ns pigeon-scoops.api
  (:require [cognitect.transit :as transit]))

(def base-url
  (if ^boolean goog.DEBUG
    "http://localhost:8080/v1"                              ;; Development URL
    "https://api.pigeon-scoops.com/v1"))                    ;; Production URL

(defn make-headers [{:keys [token body]}]
  (cond-> {:Accept "application/transit+json"}
          body (assoc :Content-Type "application/transit+json")
          token (assoc :Authorization (str "Bearer " token))))

(defn reject-error [response]
  (-> response
      (.then #(if (.-ok %)
                %
                (js/Promise.reject %)))))

(defn encode-body [body]
  (let [writer (transit/writer :json)
        body (update-keys body (comp keyword name))]
    (transit/write writer body)))

(defn do-fetch [{:keys [method url body] :as request}]
  (-> (js/fetch (str base-url url)
                (clj->js (cond-> {:method  (name method)
                                  :headers (make-headers request)}
                                 body (assoc :body (encode-body body)))))
      (reject-error)))

(defn has-response-body? [{:keys [method]}]
  (if (#{:GET :POST} method)
    :content
    :no-content))

(defmulti fetch-request has-response-body?)

(defmethod fetch-request :content [request]
  (let [reader (transit/reader :json)]
    (-> (do-fetch request)
        (.then #(.text %))
        (.then (partial transit/read reader)))))

(defmethod fetch-request :no-content [request]
  (do-fetch request))

(defn get-constants []
  (fetch-request {:method :GET :url "/constants"}))

(defn get-groceries [token]
  (fetch-request {:method :GET :url "/groceries" :token token}))

(defn get-grocery [token grocery-id]
  (fetch-request {:method :GET :url (str "/groceries/" grocery-id) :token token}))

(defn create-grocery [token grocery]
  (fetch-request {:method :POST :url "/groceries" :token token :body grocery}))

(defn update-grocery [token {:grocery/keys [id] :as grocery}]
  (fetch-request {:method :PUT :url (str "/groceries/" id) :token token :body grocery}))

(defn delete-grocery [token grocery-id]
  (fetch-request {:method :DELETE :url (str "/groceries/" grocery-id) :token token}))

(defn create-grocery-unit [token grocery-id grocery-unit]
  (fetch-request {:method :POST :url (str "/groceries/" grocery-id "/units") :body grocery-unit :token token}))

(defn update-grocery-unit [token grocery-id grocery-unit]
  (fetch-request {:method :PUT :url (str "/groceries/" grocery-id "/units") :body grocery-unit :token token}))

(defn delete-grocery-unit [token grocery-id grocery-unit-id]
  (fetch-request {:method :DELETE :url (str "/groceries/" grocery-id "/units") :body {:id grocery-unit-id} :token token}))

(defn get-recipes [token]
  (fetch-request {:method :GET :url "/recipes" :token token}))

(defn get-recipe [token recipe-id {:keys [amount amount-unit]}]
  (let [query-params (when amount
                       (js/URLSearchParams.
                         (clj->js {:amount      amount
                                   :amount-unit (str (namespace amount-unit) "/" (name amount-unit))})))]
    (fetch-request {:method :GET :url (str "/recipes/" recipe-id "?" (or query-params "")) :token token})))

(defn create-recipe [token recipe]
  (fetch-request {:method :POST :url "/recipes" :token token :body recipe}))

(defn update-recipe [token {:recipe/keys [id] :as recipe}]
  (fetch-request {:method :PUT :url (str "/recipes/" id) :token token :body recipe}))

(defn delete-recipe [token recipe-id]
  (fetch-request {:method :DELETE :url (str "/recipes/" recipe-id) :token token}))

(defn create-ingredient [token recipe-id ingredient]
  (fetch-request {:method :POST :url (str "/recipes/" recipe-id "/ingredients") :body ingredient :token token}))

(defn update-ingredient [token recipe-id ingredient]
  (fetch-request {:method :PUT :url (str "/recipes/" recipe-id "/ingredients") :body ingredient :token token}))

(defn delete-ingredient [token recipe-id ingredient-id]
  (fetch-request {:method :DELETE :url (str "/recipes/" recipe-id "/ingredients") :body {:id ingredient-id} :token token}))

(defn get-orders [token]
  (fetch-request {:method :GET :url "/orders" :token token}))

(defn get-order [token order-id]
  (fetch-request {:method :GET :url (str "/orders/" order-id) :token token}))

(defn create-order [token order]
  (fetch-request {:method :POST :url "/orders" :token token :body order}))

(defn update-order [token {:user-order/keys [id] :as order}]
  (fetch-request {:method :PUT :url (str "/orders/" id) :token token :body order}))

(defn delete-order [token order-id]
  (fetch-request {:method :DELETE :url (str "/orders/" order-id) :token token}))

(defn create-item [token order-id item]
  (fetch-request {:method :POST :url (str "/orders/" order-id "/items") :body item :token token}))

(defn update-item [token order-id item]
  (fetch-request {:method :PUT :url (str "/orders/" order-id "/items") :body item :token token}))

(defn delete-item [token order-id item-id]
  (fetch-request {:method :DELETE :url (str "/orders/" order-id "/items") :body {:id item-id} :token token}))
