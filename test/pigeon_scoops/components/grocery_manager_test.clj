(ns pigeon-scoops.components.grocery-manager-test
  (:require [clojure.test :as t]
            [pigeon-scoops.components.grocery-manager :as gm]
            [pigeon-scoops.spec.groceries :as gs]
            [pigeon-scoops.units.common :as u]
            [pigeon-scoops.units.mass :as mass]
            [pigeon-scoops.units.volume :as vol]))

(def grocery-item
  #::gs{:type        ::gs/milk
        :description "moo moo juice"
        :units       [#::gs{:source           "dark market"
                            :unit-cost        6.5
                            :unit-volume      1.0
                            :unit-volume-type ::vol/gal}
                      #::gs{:source         "dark market"
                            :unit-cost      3.25
                            :unit-mass      1.95
                            :unit-mass-type ::mass/kg}]})

(def eggs-12
  #::gs{:source           "star market"
        :unit-common      12
        :unit-common-type ::u/unit
        :unit-cost        4.99})

(def eggs-18
  #::gs{:source           "star market"
        :unit-common      18
        :unit-common-type ::u/unit
        :unit-cost        7.39})

(def common-unit-grocery-item
  #::gs{:type        ::gs/egg-yolk
        :description "need to figure out what to do with whites"
        :units       [eggs-12 eggs-18]})

(def half-gal
  #::gs{:source           "star market"
        :unit-volume      0.5
        :unit-volume-type ::vol/gal
        :unit-mass        1.94
        :unit-mass-type   ::mass/kg
        :unit-cost        7})

(def quart
  #::gs{:source           "star market"
        :unit-volume      1
        :unit-volume-type ::vol/qt
        :unit-mass        968
        :unit-mass-type   ::mass/g
        :unit-cost        4.5})

(def pint
  #::gs{:source           "star market"
        :unit-volume      1
        :unit-volume-type ::vol/pt
        :unit-mass        484
        :unit-mass-type   ::mass/g
        :unit-cost        2.8})

(def mass-volume-unit-grocery-item
  #::gs{:units [half-gal quart pint]
        :type  ::gs/half-and-half})

(def mass-only-unit-grocery-item
  #::gs{:units (map #(dissoc % ::gs/unit-volume ::gs/unit-volume-type) [half-gal quart pint])
        :type  ::gs/half-and-half})

(def no-units-grocery-item
  #::gs{:units [] :type ::gs/salt})

(t/deftest get-grocery-unit-for-amount
  (t/testing "smallest possible grocery unit is returned"
    (t/are [amount amount-unit item expected]
      (= (gm/get-grocery-unit-for-amount amount amount-unit item) expected)
      1 ::u/unit common-unit-grocery-item eggs-12
      12 ::u/unit common-unit-grocery-item eggs-12
      13 ::u/unit common-unit-grocery-item eggs-18
      20 ::u/unit common-unit-grocery-item eggs-18
      36 ::u/unit common-unit-grocery-item eggs-18
      1 ::vol/c mass-volume-unit-grocery-item pint
      4 ::mass/kg mass-volume-unit-grocery-item half-gal
      4 ::u/pinch no-units-grocery-item nil
      4 ::vol/qt mass-only-unit-grocery-item nil)))

(t/deftest divide-grocery-test
  (t/testing "an amount can be divided into a set of unit amounts"
    (t/are [amount amount-unit item expected]
      (= (gm/divide-grocery amount amount-unit item) (assoc item ::gs/units expected
                                                                 ::gs/amount-needed amount
                                                                 ::gs/amount-needed-unit amount-unit))
      0 ::u/unit common-unit-grocery-item nil
      -1 ::u/unit common-unit-grocery-item nil
      4 ::u/pinch no-units-grocery-item nil
      1 ::u/unit common-unit-grocery-item [(assoc eggs-12 ::gs/unit-purchase-quantity 1)]
      12 ::u/unit common-unit-grocery-item [(assoc eggs-12 ::gs/unit-purchase-quantity 1)]
      13 ::u/unit common-unit-grocery-item [(assoc eggs-18 ::gs/unit-purchase-quantity 1)]
      20 ::u/unit common-unit-grocery-item [(assoc eggs-18 ::gs/unit-purchase-quantity 1)
                                            (assoc eggs-12 ::gs/unit-purchase-quantity 1)]
      36 ::u/unit common-unit-grocery-item [(assoc eggs-18 ::gs/unit-purchase-quantity 2)]
      1 ::vol/c mass-volume-unit-grocery-item [(assoc pint ::gs/unit-purchase-quantity 1)]
      4 ::mass/kg mass-volume-unit-grocery-item [(assoc half-gal ::gs/unit-purchase-quantity 2)
                                                 (assoc pint ::gs/unit-purchase-quantity 1)])))
