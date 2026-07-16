# Governance

`cloud-itonami-isic-4540` is an OSS open-business blueprint for
community motorcycle dealership/workshop operations coordination.

## Maintainers
Maintainers may merge changes that preserve these invariants:
- a proposal the governor refuses is never committed.
- the MotorcycleOpsGovernor remains independent of the advisor.
- hard policy violations (an unverified sale/repair-order account, a
  non-`:propose` effect, or content that finalizes a roadworthiness-
  clearance decision) cannot be overridden by human approval.
- no proposal op ever directly finalizes a roadworthiness-clearance
  decision -- that authority never enters this actor's closed op
  allowlist.
- `:flag-safety-concern` always escalates to a human, at every phase,
  with no exception.
- every commit, hold and approval path is auditable.
- sensitive customer and payment data stays outside Git.

## Decision Records
Architecture decisions live in `docs/adr/`. Changes to the trust
model, storage contract, public business model, operator
certification or license should add or update an ADR.

## Operator Governance
Anyone may fork and operate independently. itonami.cloud
certification is a separate trust mark and should require security,
road-safety, audit and data-flow review.

Certified operators can lose certification for:
- bypassing the roadworthiness-clearance scope exclusion
- mishandling customer or payment data
- misrepresenting certification status
- failing to respond to safety-concern flags
