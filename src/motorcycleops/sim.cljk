(ns motorcycleops.sim
  "Demo driver -- `clojure -M:run`. Walks a clean service-record
  logging request through intake -> advise -> govern -> decide ->
  approval -> commit at phase 1 (assisted-logging, always approval),
  then re-runs the same op at phase 3 (supervised-auto, clean + high
  confidence -> auto-commit), then a service-scheduling request, a
  low-cost parts-order coordination request (both auto-commit clean at
  phase 3), a high-cost parts-order (ALWAYS escalates regardless of
  phase/confidence via the governor's cost threshold), then a
  safety-concern flag (ALWAYS escalates, at any phase -- approve, then
  commit), then HARD-hold scenarios: an unregistered account, an
  account registered but not yet verified, a proposal whose own
  `:effect` is not `:propose`, and a proposal that has drifted into
  the permanently-excluded roadworthiness-clearance-finalization
  scope."
  (:require [langgraph.graph :as g]
            [motorcycleops.advisor :as advisor]
            [motorcycleops.store :as store]
            [motorcycleops.operation :as op]))

(defn- exec-op [actor tid request context]
  (g/run* actor {:request request :context context} {:thread-id tid}))

(defn- approve! [actor tid]
  (g/run* actor {:approval {:status :approved :by "workshop-coordinator-1"}} {:thread-id tid :resume? true}))

(defn -main [& _]
  (let [db (store/seed-db)
        coordinator-phase-1 {:actor-id "coord-1" :actor-role :workshop-coordinator :phase 1}
        coordinator-phase-3 {:actor-id "coord-1" :actor-role :workshop-coordinator :phase 3}
        actor (op/build db)]

    (println "== log-service-record account-1 (phase 1, escalates -- human approves) ==")
    (let [r (exec-op actor "t1" {:op :log-service-record :account-id "account-1"
                                  :patch {:motorcycle-id "MC-00042" :order-type :repair :parts-used ["brake-pad-set"]}} coordinator-phase-1)]
      (println r)
      (println "-- human workshop coordinator approves --")
      (println (approve! actor "t1")))

    (println "\n== log-service-record account-1 (phase 3, clean -- auto-commits) ==")
    (println (exec-op actor "t2" {:op :log-service-record :account-id "account-1"
                                  :patch {:motorcycle-id "MC-00099" :order-type :sale :price 890000}} coordinator-phase-3))

    (println "\n== schedule-service-operation account-1 (phase 3, clean -- auto-commits) ==")
    (println (exec-op actor "t3" {:op :schedule-service-operation :account-id "account-1"
                                  :patch {:bay "bay-2" :technician "tech-7" :window "2026-07-20T09:00"}} coordinator-phase-3))

    (println "\n== coordinate-parts-order account-1, low cost (phase 3, clean -- auto-commits) ==")
    (println (exec-op actor "t4" {:op :coordinate-parts-order :account-id "account-1"
                                  :patch {:supplier "Kanda Moto Parts" :part "chain-and-sprocket-kit" :cost 42000}} coordinator-phase-3))

    (println "\n== coordinate-parts-order account-1, high cost (ALWAYS escalates via cost threshold) ==")
    (let [r (exec-op actor "t5" {:op :coordinate-parts-order :account-id "account-1"
                                 :patch {:supplier "Kanda Moto Parts" :part "replacement-engine-block" :cost 480000}} coordinator-phase-3)]
      (println r)
      (println "-- human workshop coordinator approves --")
      (println (approve! actor "t5")))

    (println "\n== flag-safety-concern account-1 (ALWAYS escalates, even at phase 3) ==")
    (let [r (exec-op actor "t6" {:op :flag-safety-concern :account-id "account-1"
                                 :patch {:concern "front brake lever play reported by customer at pickup" :confidence 0.9}} coordinator-phase-3)]
      (println r)
      (println "-- human workshop coordinator reviews & approves --")
      (println (approve! actor "t6")))

    (println "\n== log-service-record account-99 (unregistered account -> HARD hold) ==")
    (println (exec-op actor "t7" {:op :log-service-record :account-id "account-99"
                                  :patch {:motorcycle-id "MC-00007"}} coordinator-phase-3))

    (println "\n== log-service-record account-3 (registered but unverified -> HARD hold) ==")
    (println (exec-op actor "t8" {:op :log-service-record :account-id "account-3"
                                  :patch {:motorcycle-id "MC-00012"}} coordinator-phase-3))

    (println "\n== schedule-service-operation account-1, advisor attempts direct actuation (:effect :commit) -> HARD hold ==")
    (let [actor-direct (op/build db {:advisor (reify advisor/Advisor
                                                (-advise [_ _ req]
                                                  (assoc (advisor/infer nil req) :effect :commit)))})]
      (println (exec-op actor-direct "t9" {:op :schedule-service-operation :account-id "account-1"
                                           :patch {:bay "bay-2 take 2"}} coordinator-phase-3)))

    (println "\n== log-service-record account-1, advisor drifts into roadworthiness-clearance-finalization scope -> HARD hold, permanent ==")
    (println (exec-op actor "t10" {:op :log-service-record :account-id "account-1"
                                    :out-of-scope? true
                                    :patch {}} coordinator-phase-3))

    (println "\n== audit ledger ==")
    (doseq [f (store/ledger db)] (println f))

    (println "\n== committed service log ==")
    (doseq [r (store/service-log db)] (println r))))
