(ns pigeon-scoops.components.number-field
  (:require [uix.core :as uix :refer [$ defui]]
            ["@base-ui-components/react/number-field" :refer [NumberField]]))

(defui cursor-grow-icon []
       ($ :svg {:width   "26"
                :height  "14"
                :viewBox "0 0 24 14"
                :fill    "black"
                :stroke  "white"
                :xmlns   "http://www.w3.org/2000/svg"}
          ($ :path {:d "M19.5 5.5L6.49737 5.51844V2L1 6.9999L6.5 12L6.49737 8.5L19.5 8.5V12L25 6.9999L19.5 2V5.5Z"})))

(defui plus-icon []
       ($ :svg {:width        "10"
                :height       "10"
                :viewBox      "0 0 10 10"
                :fill         "none"
                :stroke       "currentcolor"
                :stroke-width "1.6"
                :xmlns        "http://www.w3.org/2000/svg"}
          ($ :path {:d "M0 5H5M10 5H5M5 5V0M5 5V10"})))

(defui minus-icon []
       ($ :svg {:width        "10"
                :height       "10"
                :viewBox      "0 0 10 10"
                :fill         "none"
                :stroke       "currentcolor"
                :stroke-width "1.6"
                :xmlns        "http://www.w3.org/2000/svg"}
          ($ :path {:d "M0 5H10"})))

(defui number-field [{:keys [value set-value! label hide-controls?]}]
       (let [id (uix/use-id)]
         ($ NumberField.Root {:id              id
                              :value           value
                              :on-value-change #(set-value! %1)
                              :className       "Field"}
            ($ NumberField.ScrubArea {:className "ScrubArea"}
               (when label
                 ($ :label {:htmlFor id :className "Label"} "Amount"))
               ($ NumberField.ScrubAreaCursor {:className "ScrubAreaCursor"}
                  ($ cursor-grow-icon)))
            ($ NumberField.Group {:className "Group"}
               (when-not hide-controls?
                 ($ NumberField.Decrement {:className "Decrement"} ($ minus-icon)))
               ($ NumberField.Input {:className "Input"})
               (when-not hide-controls?
                 ($ NumberField.Increment {:className "Increment"} ($ plus-icon)))))))
