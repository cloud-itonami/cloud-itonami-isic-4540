(ns motorcycleops.governor
  "MotorcycleOpsGovernor -- the independent compliance layer that earns
  the MotorcycleOpsAdvisor the right to commit. The advisor has no
  notion of whether a sale/repair-order account is actually registered
  and verified, whether its own proposed `:effect` secretly claims a
  direct actuation instead of a mere proposal, or whether it has
  silently drifted into a permanently out-of-scope decision area, so
  this MUST be a separate system able to *reject* a proposal and fall
  back to HOLD.

  This actor's scope is deliberately narrow -- ISIC-4540 SALE,
  MAINTENANCE AND REPAIR OF MOTORCYCLES AND RELATED PARTS AND
  ACCESSORIES OPERATIONS COORDINATION ONLY (sale/repair-order and
  parts-used data logging, bay/technician service-scheduling
  proposals, safety-concern flagging, parts/inventory procurement
  coordination). It NEVER performs or authorizes finalizing a
  roadworthiness-clearance decision (certifying a motorcycle safe to
  return to the road/customer after service or sale).

  Motorcycle repair/maintenance has a DIRECT road-safety dimension:
  this actor coordinates the back office around a roadworthiness-
  clearance decision, it never makes it. That exclusion is ALWAYS
  either a hard permanent block (this governor) or an always-escalate
  op (`:flag-safety-concern`) -- NEVER an auto-commit-eligible op in
  any phase. No proposal op in this actor's closed allowlist directly
  finalizes a roadworthiness-clearance decision; `:flag-safety-concern`
  only ever *surfaces a concern* for human triage.

  Three HARD checks, ALL permanent, un-overridable by any human approval:

    1. Account unverified         -- the target sale/repair-order
                                      account must exist AND be
                                      independently confirmed
                                      `:registered?`/`:verified?` in
                                      the store before ANY proposal for
                                      it may commit or even escalate.
                                      Never trusts a proposal's own
                                      claim about the account --
                                      re-derived from the account's own
                                      store record, the same 'ground
                                      truth, not self-report'
                                      discipline every sibling actor's
                                      governor uses.
    2. Effect not :propose        -- every proposal's `:effect` MUST
                                      be `:propose`. Any other effect
                                      value is, by construction, a
                                      claim to directly actuate/commit
                                      outside governance -- HARD block,
                                      not merely low-confidence.
    3. Scope exclusion            -- ANY proposal (regardless of op)
                                      whose op, rationale, summary,
                                      citations or draft value touches
                                      the ACT of finalizing a
                                      roadworthiness-clearance decision
                                      is a HARD, PERMANENT block --
                                      this actor's charter excludes
                                      that territory structurally, not
                                      as a rollout milestone. Evaluated
                                      UNCONDITIONALLY on every
                                      proposal. An op outside the
                                      closed four-op allowlist is the
                                      SAME failure mode (an advisor
                                      proposing something it was never
                                      authorized to propose) and is
                                      folded into this same check.

  IMPORTANT (self-trip discipline): `scope-excluded-terms` below are
  phrased as the FINALIZATION/EXECUTION ACTION ('finalize the
  roadworthiness clearance', 'certify the motorcycle roadworthy despite
  the known defect'), never as a bare noun ('roadworthiness', 'safety',
  'clearance', 'defect'). This actor's own legitimate happy-path
  proposals -- especially `:flag-safety-concern`, whose entire purpose
  is to talk ABOUT defect/recall/unsafe-repair concerns -- routinely
  use those bare nouns in their default rationale text. A bare-noun
  term list would self-trip the actor on its own default mock-advisor
  proposals; `governor-test` and `governor-contract-test` both assert
  this never happens.

  One ESCALATE (SOFT) gate: LLM confidence below the floor, OR the op
  is `:flag-safety-concern` (ALWAYS escalates to a human, regardless of
  confidence, regardless of how clean the proposal otherwise is), OR
  a `:coordinate-parts-order` proposal whose draft cost is above
  `parts-order-cost-threshold`. `motorcycleops.phase` independently
  agrees: `:flag-safety-concern` is never a member of any phase's
  `:auto` set either -- two layers, not one."
  (:require [clojure.string :as str]
            [motorcycleops.store :as store]))

(def confidence-floor 0.6)

(def parts-order-cost-threshold
  "Parts/inventory procurement proposals whose draft cost exceeds this
  (currency-agnostic demo units) ALWAYS require human sign-off,
  regardless of governor cleanliness or advisor confidence."
  300000)

(def allowed-ops
  "The closed proposal-op allowlist -- an op outside this set is a
  scope violation by construction (see `scope-exclusion-violations`).
  No op in this set directly finalizes a roadworthiness-clearance
  decision."
  #{:log-service-record :schedule-service-operation
    :flag-safety-concern :coordinate-parts-order})

(def always-escalate-ops
  "Ops that ALWAYS require human sign-off, clean or not."
  #{:flag-safety-concern})

(def scope-excluded-terms
  "Case-insensitive substrings that mark a proposal as attempting to
  directly FINALIZE a roadworthiness-clearance decision -- this
  actor's one permanently out-of-scope decision area. Phrased as the
  finalization/execution ACTION, never as a bare noun, so this list
  never matches inside this actor's own legitimate proposals (which
  routinely discuss defects/recalls/unsafe-repair concerns as topics
  without ever finalizing a roadworthiness clearance). Scanned across
  the proposal's op/summary/rationale/cites/value, never trusting the
  advisor's own framing of its intent."
  ["finalize the roadworthiness clearance" "finalize roadworthiness clearance"
   "finalize the roadworthiness-clearance decision" "confirm the roadworthiness clearance decision"
   "certify the motorcycle roadworthy despite the known defect" "clear the motorcycle as roadworthy despite the defect"
   "release the motorcycle despite the recall" "authorize the roadworthiness clearance"
   "waive the roadworthiness clearance requirement" "declare the motorcycle safe to ride despite the defect"
   "sign off on roadworthiness despite the open recall" "return the motorcycle to the customer despite the known defect"
   "公道走行可否を確定" "整備不良のまま公道復帰を確定" "リコール対象のまま納車可否を確定"
   "公道走行安全性の最終判断を下す" "整備完了証明を確定発行" "既知の不具合を残したまま整備完了と確定"])

;; ----------------------------- checks -----------------------------

(defn- account-unverified-violations
  "The target sale/repair-order account must exist AND be
  independently `:registered?`/`:verified?` in the store -- never
  trust the proposal's own `:account-id` claim without a store lookup."
  [{:keys [account-id]} st]
  (let [r (store/account st account-id)]
    (when-not (and r (:registered? r) (:verified? r))
      [{:rule :account-unverified
        :detail (str account-id " は未登録または未検証の販売/整備注文アカウント -- いかなる提案も進められない")}])))

(defn- effect-not-propose-violations
  "`:effect` must ALWAYS be `:propose` -- any other value is a claim
  to directly actuate/commit outside governance."
  [proposal]
  (when (not= :propose (:effect proposal))
    [{:rule :effect-not-propose
      :detail (str ":effect は :propose のみ許可されるが " (pr-str (:effect proposal)) " が提案された")}]))

(defn- text-blob
  "Flatten every advisor-authored field on a proposal into one
  lower-cased blob the scope-exclusion scan checks."
  [proposal]
  (str/lower-case (pr-str (select-keys proposal [:op :summary :rationale :cites :value]))))

(defn- scope-exclusion-violations
  "HARD, PERMANENT block: a proposal outside the closed op allowlist,
  or one whose content touches finalizing a roadworthiness-clearance
  decision, regardless of confidence or how clean every other check
  is. Evaluated UNCONDITIONALLY on every proposal."
  [proposal]
  (let [op (:op proposal)
        blob (text-blob proposal)]
    (cond
      (not (contains? allowed-ops op))
      [{:rule :op-not-allowed
        :detail (str (pr-str op) " は許可された操作(closed allowlist)に含まれない")}]

      (some #(str/includes? blob %) scope-excluded-terms)
      [{:rule :scope-excluded
        :detail "公道走行可否(ロードワージネス)クリアランスの最終判断に踏み込む提案は永久に禁止"}])))

;; ----------------------------- escalation -----------------------------

(defn- parts-order-cost
  "Best-effort numeric cost read from a :coordinate-parts-order
  proposal's draft `:value` -- never trusted for hard checks, only for
  the soft cost-threshold escalation gate."
  [proposal]
  (let [v (:value proposal)]
    (or (:cost v) (:amount v) (:estimated-cost v) 0)))

(defn- high-stakes?
  "True when this proposal ALWAYS requires human sign-off: any
  `always-escalate-ops` member, or a parts-order proposal whose draft
  cost exceeds `parts-order-cost-threshold`."
  [proposal]
  (boolean
   (or (always-escalate-ops (:op proposal))
       (and (= :coordinate-parts-order (:op proposal))
            (> (parts-order-cost proposal) parts-order-cost-threshold)))))

(defn check
  "Censors a MotorcycleOpsAdvisor proposal against the governor rules.
  Returns {:ok? bool :violations [..] :confidence c :escalate? bool
  :high-stakes? bool :hard? bool}."
  [request _context proposal store]
  (let [account-id (or (:account-id proposal) (:account-id request))
        hard (into []
                   (concat (account-unverified-violations {:account-id account-id} store)
                           (effect-not-propose-violations proposal)
                           (scope-exclusion-violations proposal)))
        conf (:confidence proposal 0.0)
        low? (< conf confidence-floor)
        stakes? (high-stakes? proposal)
        hard? (boolean (seq hard))]
    {:ok?          (and (not hard?) (not low?) (not stakes?))
     :violations   hard
     :confidence   conf
     :hard?        hard?
     :escalate?    (and (not hard?) (or low? stakes?))
     :high-stakes? stakes?}))

(defn hold-fact
  "The audit fact written when a proposal is rejected (HOLD)."
  [request context verdict]
  {:t          :governor-hold
   :op         (:op request)
   :actor      (:actor-id context)
   :account-id (:account-id request)
   :disposition :hold
   :basis      (mapv :rule (:violations verdict))
   :violations (:violations verdict)
   :confidence (:confidence verdict)})
