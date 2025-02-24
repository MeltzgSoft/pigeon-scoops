(ns pigeon-scoops.utils
  (:require [clojure.set :as set]))

(defn determine-ops [id-key original-entities updated-entities]
  (let [original-ids (set (map id-key original-entities))
        updated-ids (set (map id-key updated-entities))]
    (assoc
      (group-by #(if (original-ids (id-key %)) :update :new) updated-entities)
      :delete (set/difference original-ids updated-ids))))
