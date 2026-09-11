(ns motorcycleops.advisor
  "MotorcycleOpsAdvisor -- the *contained intelligence node* for the
  ISIC-4540 SALE, MAINTENANCE AND REPAIR OF MOTORCYCLES AND RELATED
  PARTS AND ACCESSORIES operations-coordination actor.

  It drafts exactly four kinds of back-office proposal from a closed
  allowlist: sale/repair-order (and parts-used) logging, bay/
  technician service-scheduling, safety-concern flagging, and parts/
  inventory procurement coordination. CRITICAL: it is a
  smart-but-untrusted advisor. It returns a *proposal* (with a
  rationale + the fields it cited), never a committed record and NEVER
  a direct actuation -- every proposal's `:effect` is always
  `:propose`. Every output is censored downstream by
  `motorcycleops.governor` before anything touches the SSoT.

  This advisor NEVER finalizes a roadworthiness-clearance decision
  (certifying a motorcycle safe to return to the road/customer after
  service or sale) -- that is permanently out of scope for this actor,
  not merely un-implemented. Motorcycle repair/maintenance has a
  direct road-safety dimension, so `motorcycleops.governor`'s
  `scope-exclusion-violations` independently re-scans every proposal
  for exactly this failure mode (a compromised or confused advisor
  drifting into scope it must never touch) and HARD-holds it,
  regardless of confidence or op.

  Like every sibling actor's advisor, this is a deterministic mock so
  the actor graph runs offline and the governor contract is exercised
  end-to-end. In production this calls a real LLM (kotoba-llm or
  equivalent) with the same proposal shape.

  Proposal shape (all kinds):
    {:op         kw             ; echoes the request op
     :account-id str
     :summary    str            ; human-facing draft / finding
     :rationale  str            ; why -- SCANNED by the scope-exclusion gate
     :cites      [str ..]       ; facts/sources the advisor used -- SCANNED too
     :effect     :propose       ; ALWAYS :propose -- never a direct actuation
     :value      map            ; the draft payload a human/system would review
     :confidence 0..1}")

(defprotocol Advisor
  (-advise [advisor store request] "store + request -> proposal map"))

;; ----------------------------- proposal generators -----------------------------

(defn- propose-service-record
  "Draft a sale/repair-order (and parts-used) log entry. Pure
  transaction/parts metadata logging -- never a roadworthiness-
  clearance determination."
  [_db {:keys [account-id patch]}]
  {:op         :log-service-record
   :account-id account-id
   :summary    (str account-id " の販売/整備注文記録(部品使用含む)を記録: " (pr-str (keys patch)))
   :rationale  "販売・整備注文・使用部品のメタデータ記録のみ。公道走行可否(ロードワージネス)クリアランスの確定とは無関係。"
   :cites      [account-id]
   :effect     :propose
   :value      (merge {:account-id account-id} patch)
   :confidence 0.93})

(defn- propose-service-operation
  "Draft a bay/technician service-scheduling proposal (an internal ops
  calendar entry, never a binding completion commitment or a
  roadworthiness-clearance decision)."
  [_db {:keys [account-id patch]}]
  {:op         :schedule-service-operation
   :account-id account-id
   :summary    (str account-id " のベイ/技術者の整備スケジュール調整を提案: " (pr-str (keys patch)))
   :rationale  "整備ベイと技術者の社内スケジュール調整提案のみ。整備完了後に公道走行を許可するかどうかの判断を下すものではない。"
   :cites      [account-id]
   :effect     :propose
   :value      (merge {:account-id account-id} patch)
   :confidence 0.88})

(defn- propose-safety-concern
  "Surface a defect, recall, or unsafe-repair concern for HUMAN
  triage. This op ALWAYS escalates in `motorcycleops.governor` --
  never auto-committed at any phase -- regardless of how confident
  the advisor is that the concern is real."
  [_db {:keys [account-id patch]}]
  {:op         :flag-safety-concern
   :account-id account-id
   :summary    (str account-id " の安全性懸念フラグ: " (pr-str (:concern patch "unknown")))
   :rationale  "整備不良の疑い・リコール対象・不適切な修理に関する観察事実の報告。公道走行可否の最終判断は常に人間が行う。"
   :cites      [account-id]
   :effect     :propose
   :value      (merge {:account-id account-id} patch)
   :confidence (or (:confidence patch) 0.85)})

(defn- propose-parts-order
  "Draft a parts/inventory procurement coordination proposal
  (logistics/ordering coordination only, never a binding purchase
  commitment or payment approval)."
  [_db {:keys [account-id patch]}]
  {:op         :coordinate-parts-order
   :account-id account-id
   :summary    (str account-id " の部品調達コーディネートを提案: " (pr-str (keys patch)))
   :rationale  "補修部品の調達コーディネート提案のみ。契約締結や支払承認の権限は持たない。"
   :cites      [account-id]
   :effect     :propose
   :value      (merge {:account-id account-id} patch)
   :confidence 0.90})

;; ----------------------------- default mock advisor -----------------------------

(defn infer
  "Mock advisor: routes to the correct proposal generator."
  [_db {:keys [op out-of-scope?] :as request}]
  (let [proposal (case op
                   :log-service-record (propose-service-record _db request)
                   :schedule-service-operation (propose-service-operation _db request)
                   :flag-safety-concern (propose-safety-concern _db request)
                   :coordinate-parts-order (propose-parts-order _db request)
                   {})]
    ;; Test hook: allow injecting scope-excluded content to exercise the
    ;; governor's scope-exclusion block end-to-end. Must be cleared before
    ;; production use.
    (if out-of-scope?
      (update proposal :rationale str " -- actually decided to finalize the roadworthiness clearance and release the motorcycle despite the recall")
      proposal)))

(defn trace
  "Audit fact for a proposal generated by this advisor."
  [_request proposal]
  {:t          :advisor-proposal
   :op         (:op proposal)
   :account-id (:account-id proposal)
   :summary    (:summary proposal)
   :confidence (:confidence proposal)})

(defn mock-advisor
  "The deterministic default advisor for offline demo/test."
  []
  (reify Advisor
    (-advise [_ _store request]
      (infer nil request))))
