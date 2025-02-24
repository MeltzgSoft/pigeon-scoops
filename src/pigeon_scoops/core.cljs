(ns pigeon-scoops.core
  (:require [pigeon-scoops.auth :refer [authenticator]]
            [pigeon-scoops.context :as ctx]
            [pigeon-scoops.grocery.context :as gctx]
            [pigeon-scoops.recipe.context :as rctx]
            [pigeon-scoops.router :refer [router-context with-router]]
            [pigeon-scoops.user-order.context :as octx]
            [reitit.frontend.easy :as rfe]
            [uix.core :as uix :refer [$ defui]]
            [uix.dom]
            ["@auth0/auth0-react" :refer [Auth0Provider]]
            ["@mui/icons-material/Menu$default" :as MenuIcon]
            ["@mui/icons-material/Icecream$default" :as IcecreamIcon]
            ["@mui/icons-material/LocalGroceryStore$default" :as LocalGroceryStoreIcon]
            ["@mui/icons-material/MenuBook$default" :as MenuBookIcon]
            ["@mui/icons-material/Receipt$default" :as ReceiptIcon]
            ["@mui/material" :refer [AppBar
                                     Box
                                     Drawer
                                     IconButton
                                     List
                                     ListItem
                                     ListItemButton
                                     ListItemIcon
                                     ListItemText
                                     Toolbar
                                     Typography]]))

(defui app-menu-item [{:keys [text icon page on-click]}]
       ($ ListItem
          ($ ListItemButton {:href     (rfe/href page)
                             :on-click on-click}
             ($ ListItemIcon
                ($ icon))
             ($ ListItemText {:primary text}))))

(defui content []
       (let [{:keys [route]} (uix/use-context router-context)
             [menu-open? set-menu-open!] (uix/use-state false)]
         ($ Box
            ($ AppBar
               ($ Toolbar
                  ($ IconButton {:on-click #(set-menu-open! (not menu-open?))}
                     ($ MenuIcon))
                  ($ Typography
                     "Pigeon Scoops Manager")
                  ($ Box {:ml "auto"}
                     ($ authenticator))))
            ($ Drawer {:anchor   "left"
                       :open     menu-open?
                       :on-close #(set-menu-open! (not menu-open?))}
               ($ List
                  (for [[app-name app-icon app-key page] [["Groceries" LocalGroceryStoreIcon :groceries :pigeon-scoops.grocery.routes/groceries]
                                                          ["Recipes" MenuBookIcon :recipes :pigeon-scoops.recipe.routes/recipes]
                                                          ["Orders" ReceiptIcon :orders :pigeon-scoops.user-order.routes/orders]]]
                    ($ app-menu-item {:key      app-key
                                      :text     app-name
                                      :icon     app-icon
                                      :page     page
                                      :on-click #(set-menu-open! (not menu-open?))}))))
            ($ Box {:component "div"}
               ($ Toolbar)
               (when route
                 ($ (-> route :data :view) (:parameters route)))))))


(defui app []
       ($ Auth0Provider {:domain               "pigeon-scoops.us.auth0.com"
                         :client-id            "AoU9LnGWQlCbSUvjgXdHf4NZPJh0VHYD"
                         :cache-location       "localstorage"
                         :use-refresh-tokens   true
                         :authorization-params (clj->js {:redirect_uri (.. js/window -location -origin)
                                                         :scope        "openid profile email offline_access"
                                                         :audience     "https://api.pigeon-scoops.com"})}
          ($ with-router
             ($ ctx/with-constants
                ($ gctx/with-groceries
                   ($ rctx/with-recipes
                      ($ octx/with-orders
                         ($ content))))))))

(defonce root
         (uix.dom/create-root (js/document.getElementById "root")))

(defn render []
  (uix.dom/render-root ($ app) root))

(defn ^:export init []
  (render))
