(ns chem.governor-test
  (:require [clojure.test :refer [deftest is testing]]
            [chem.store :as store]
            [chem.governor :as governor]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-batch! st {:batch-id "batch-1" :name "Polymer Resin Batch A" :material-type "Epoxy"})
    st))

(deftest ok-on-clean-lab-test
  (let [st (fresh-store)
        proposal {:op :draft-lab-test :effect :propose :confidence 0.9 :stake :low}
        v (governor/check {:batch-id "batch-1"} {} proposal st)]
    (is (:ok? v))
    (is (not (:hard? v)))
    (is (not (:escalate? v)))))

(deftest ok-on-material-analysis
  (let [st (fresh-store)
        proposal {:op :record-material-analysis :effect :propose :confidence 0.85 :stake :medium}
        v (governor/check {:batch-id "batch-1"} {} proposal st)]
    (is (:ok? v))
    (is (not (:hard? v)))
    (is (not (:escalate? v)))))

(deftest ok-on-batch-review-scheduling
  (let [st (fresh-store)
        proposal {:op :schedule-batch-review :effect :propose :confidence 0.8 :stake :low}
        v (governor/check {:batch-id "batch-1"} {} proposal st)]
    (is (:ok? v))
    (is (not (:hard? v)))
    (is (not (:escalate? v)))))

(deftest hard-on-unregistered-batch
  (let [st (fresh-store)
        proposal {:op :draft-lab-test :effect :propose :confidence 0.9 :stake :low}
        v (governor/check {:batch-id "no-such-batch"} {} proposal st)]
    (is (:hard? v))
    (is (some #(= :no-batch (:rule %)) (:violations v)))))

(deftest hard-on-no-actuation-violation
  (let [st (fresh-store)
        proposal {:op :draft-lab-test :effect :direct-write :confidence 0.9 :stake :low}
        v (governor/check {:batch-id "batch-1"} {} proposal st)]
    (is (:hard? v))
    (is (some #(= :no-actuation (:rule %)) (:violations v)))))

(deftest escalates-on-chemical-hazard
  (let [st (fresh-store)
        proposal {:op :flag-chemical-hazard :effect :propose :confidence 0.9 :stake :high}
        v (governor/check {:batch-id "batch-1"} {} proposal st)]
    (is (:escalate? v))
    (is (not (:hard? v)))))

(deftest escalates-on-low-confidence
  (let [st (fresh-store)
        proposal {:op :draft-lab-test :effect :propose :confidence 0.2 :stake :low}
        v (governor/check {:batch-id "batch-1"} {} proposal st)]
    (is (:escalate? v))
    (is (not (:hard? v)))))

(deftest store-records-and-ledger-append-only
  (let [st (fresh-store)]
    (store/commit-record! st {:batch-id "batch-1" :op :draft-lab-test})
    (store/append-ledger! st {:disposition :commit})
    (is (= 1 (count (store/records-of st "batch-1"))))
    (is (= 1 (count (store/ledger st))))))
