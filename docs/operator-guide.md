# Operator Guide

## First Deployment
1. Register operator, dealership/workshop locations, staff and
   sale/repair-order accounts (each must be independently
   registered AND verified before any proposal for it can commit or
   escalate).
2. Import existing service and parts-order history.
3. Run read-only service-record and scheduling dry-runs.
4. Configure the rollout phase (0-3) and human sign-off paths.
5. Publish a dry-run audit export.

## Minimum Production Controls
- account registration + verification before any proposal for that
  account may commit or escalate
- governor gate on every proposal before commit
- human sign-off for `:flag-safety-concern` (always, every phase) and
  for `:coordinate-parts-order` proposals above the cost threshold
- the roadworthiness-clearance scope exclusion is permanent and
  un-overridable -- no configuration flag, phase, or human approval
  can re-enable it
- audit export for every commit, hold and approval
- backup manual sale/repair-order process

## Certification
Certified operators must prove governor integrity (the
roadworthiness-clearance scope exclusion is never bypassed),
account-verification discipline, evidence-backed service records and
human review for safety-concern and high-cost parts-order proposals.
