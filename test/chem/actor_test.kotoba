(ns chem.actor-test
  (:require [clojure.test :refer [deftest is testing]]
            [chem.store :as store]
            [chem.advisor :as advisor]
            [chem.actor :as actor]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-batch! st {:batch-id "batch-1" :name "Polymer Resin Batch A" :material-type "Epoxy"})
    st))

(deftest run-request-accepts-lab-test
  (let [st (fresh-store)
        graph (actor/build-graph {:store st :advisor (advisor/mock-advisor)})
        result (actor/run-request! graph
                                  {:batch-id "batch-1" :op :draft-lab-test :stake :low}
                                  {}
                                  "thread-1")]
    (is (= :done (:status result)))
    (is (= 1 (count (store/records-of st "batch-1"))))))

(deftest run-request-escalates-chemical-hazard
  (let [st (fresh-store)
        graph (actor/build-graph {:store st :advisor (advisor/mock-advisor)})
        result (actor/run-request! graph
                                  {:batch-id "batch-1" :op :flag-chemical-hazard :stake :high}
                                  {}
                                  "thread-2")]
    (is (= :interrupted (:status result)))
    (is (empty? (store/records-of st "batch-1")))))

(deftest run-request-rejects-unregistered-batch
  (let [st (fresh-store)
        graph (actor/build-graph {:store st :advisor (advisor/mock-advisor)})
        result (actor/run-request! graph
                                  {:batch-id "no-such-batch" :op :draft-lab-test :stake :low}
                                  {}
                                  "thread-3")]
    (is (= :done (:status result)))
    (is (empty? (store/records-of st "no-such-batch")))))

(deftest approve-resumes-and-commits
  (let [st (fresh-store)
        graph (actor/build-graph {:store st :advisor (advisor/mock-advisor)})
        result1 (actor/run-request! graph
                                   {:batch-id "batch-1" :op :flag-chemical-hazard :stake :high}
                                   {}
                                   "thread-4")]
    (is (= :interrupted (:status result1)))
    (is (empty? (store/records-of st "batch-1")))

    ;; approve and resume
    (let [result2 (actor/approve! graph "thread-4")]
      (is (= :done (:status result2)))
      (is (= 1 (count (store/records-of st "batch-1")))))))

(deftest audit-ledger-records-all-events
  (let [st (fresh-store)
        graph (actor/build-graph {:store st :advisor (advisor/mock-advisor)})
        _ (actor/run-request! graph
                             {:batch-id "batch-1" :op :draft-lab-test :stake :low}
                             {}
                             "thread-5")]
    (is (>= (count (store/ledger st)) 1))))