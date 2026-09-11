(ns motorcycleops.phase-test
  "Unit tests of `motorcycleops.phase` rollout logic."
  (:require [clojure.test :refer [deftest is testing]]
            [motorcycleops.phase :as phase]))

(def clean-verdict {:hard? false :escalate? false})
(def low-conf-verdict {:hard? false :escalate? true})
(def hard-verdict {:hard? true :escalate? false})

(deftest phase-0-read-only
  (testing "phase 0 allows no writes"
    (doseq [op [:log-service-record :schedule-service-operation
                :flag-safety-concern :coordinate-parts-order]]
      (let [{:keys [disposition]} (phase/gate 0 {:op op} :commit)]
        (is (= :hold disposition)
            (str "phase 0 must hold all ops including " op))))))

(deftest phase-1-service-record-only
  (testing "phase 1 allows only service-record logging, requires approval"
    (let [{:keys [disposition reason]} (phase/gate 1 {:op :log-service-record} :commit)]
      (is (= :escalate disposition))
      (is (= :phase-approval reason)))
    (let [{:keys [disposition]} (phase/gate 1 {:op :schedule-service-operation} :commit)]
      (is (= :hold disposition)))))

(deftest phase-2-adds-coordination-ops
  (testing "phase 2 allows coordination ops, still requires approval"
    (doseq [op [:log-service-record :schedule-service-operation :coordinate-parts-order]]
      (let [{:keys [disposition]} (phase/gate 2 {:op op} :commit)]
        (is (= :escalate disposition)
            (str "phase 2 op " op " requires approval"))))))

(deftest phase-3-auto-commits-clean-ops
  (testing "phase 3 auto-commits clean, high-conf non-safety-concern ops"
    (let [{:keys [disposition]} (phase/gate 3 {:op :log-service-record} :commit)]
      (is (= :commit disposition)))
    (let [{:keys [disposition]} (phase/gate 3 {:op :schedule-service-operation} :commit)]
      (is (= :commit disposition)))
    (let [{:keys [disposition]} (phase/gate 3 {:op :coordinate-parts-order} :commit)]
      (is (= :commit disposition)))))

(deftest safety-concern-holds-when-not-enabled
  (testing ":flag-safety-concern holds in phases 0-2 (not yet enabled)"
    (doseq [ph [0 1 2]]
      (let [{:keys [disposition]} (phase/gate ph {:op :flag-safety-concern} :escalate)]
        (is (= :hold disposition)
            (str "phase " ph " has not enabled flag-safety-concern yet"))))))

(deftest safety-concern-escalates-when-enabled
  (testing ":flag-safety-concern ALWAYS escalates when enabled, even if governor says commit"
    (let [{:keys [disposition]} (phase/gate 3 {:op :flag-safety-concern} :commit)]
      (is (= :escalate disposition)
          "phase 3 must escalate safety concerns regardless of governor disposition"))))

(deftest high-cost-parts-order-stays-escalate-through-phase-gate
  (testing "a governor-side escalate (e.g. cost-threshold) survives the phase gate even though the op is auto-eligible"
    (let [{:keys [disposition]} (phase/gate 3 {:op :coordinate-parts-order} :escalate)]
      (is (= :escalate disposition)
          "a governor escalate must not be silently promoted to commit by phase auto-eligibility"))))

(deftest hard-hold-always-wins
  (testing "a governor HARD hold stays HOLD regardless of phase"
    (doseq [ph [0 1 2 3]]
      (let [{:keys [disposition]} (phase/gate ph {:op :log-service-record} :hold)]
        (is (= :hold disposition)
            (str "phase " ph " must respect governor HARD hold"))))))

(deftest verdict->disposition-maps-correctly
  (testing "verdict->disposition correctly translates governor verdict to base disposition"
    (is (= :hold (phase/verdict->disposition {:hard? true :escalate? false})))
    (is (= :escalate (phase/verdict->disposition {:hard? false :escalate? true})))
    (is (= :commit (phase/verdict->disposition {:hard? false :escalate? false})))))

(deftest flag-safety-concern-never-in-any-auto-set
  (testing ":flag-safety-concern must never be a member of any phase's :auto set (structural invariant)"
    (doseq [[ph {:keys [auto]}] phase/phases]
      (is (not (contains? auto :flag-safety-concern))
          (str "phase " ph " must never auto-commit :flag-safety-concern")))))
