# cloud-itonami-isic-4540

Open Business Blueprint for **ISIC Rev.5 4540**: sale, maintenance and
repair of motorcycles and related parts and accessories.

This repository designs a forkable OSS business for community
motorcycle dealership/workshop operations coordination: sale/repair-
order and parts-used logging, bay/technician service scheduling,
safety-concern flagging, and parts/inventory procurement coordination
-- run by a qualified operator so a dealership/workshop keeps its own
auditable service and safety-concern history instead of a closed
shop-management platform.

## Scope note: back-office coordination, not roadworthiness authority

Motorcycle repair/maintenance has a **direct road-safety dimension**:
a badly serviced motorcycle can kill its rider. This actor
deliberately does NOT hold roadworthiness-clearance authority --
`motorcycleops.governor` HARD-blocks, permanently, any proposal that
attempts to finalize a roadworthiness-clearance decision (certifying a
motorcycle safe to return to the road/customer after service or
sale), no matter the op, no matter the confidence. That decision
always stays with a qualified human technician/inspector. This actor
coordinates the back office around that decision; it never makes it.

Distinct from `cloud-itonami-isic-4520` (maintenance and repair of
motor vehicles -- cars/light trucks) and `cloud-itonami-isic-4530`
(sale of motor-vehicle parts and accessories), both expected as
sibling actors in this fleet's ISIC 45xx coverage: this actor is
scoped specifically to motorcycles and their related parts/
accessories, a distinct ISIC 4540 class with its own dealership/
workshop trade patterns and parts catalog.

## Core Contract

```text
intake + identity + registered/verified sale-repair-order account + request
        |
        v
MotorcycleOpsAdvisor -> MotorcycleOpsGovernor -> commit, hold, or human approval
        |
        v
service record + service-scheduling proposal + safety-concern flag + parts-order coordination + audit ledger
```

No automated advice can commit a record for an unregistered/
unverified account, claim a direct actuation (`:effect` other than
`:propose`), or finalize a roadworthiness-clearance decision. The
governor's scope-exclusion check is HARD and permanent, independent
of confidence, phase, or op.

## Proposal ops (closed allowlist)

- `:log-service-record` -- sale/repair-order and parts-used data
  logging
- `:schedule-service-operation` -- bay/technician service-scheduling
  proposal
- `:flag-safety-concern` -- surface a defect/recall/unsafe-repair
  concern; ALWAYS escalates to a human, at any phase
- `:coordinate-parts-order` -- parts/inventory procurement proposal;
  ALWAYS escalates above a cost threshold, regardless of phase or
  confidence

## Capability layer

Resolves via [`kotoba-lang/industry`](https://github.com/kotoba-lang/industry)
(ISIC `4540`). Implemented by:

- [`kotoba-lang/robotics`](https://github.com/kotoba-lang/robotics) -- missions, actions, safety-stops, telemetry proofs
- [`kotoba-lang/langgraph`](https://github.com/kotoba-lang/langgraph) -- the StateGraph actor runtime

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## Development

```bash
clojure -M:test
clojure -M:lint
clojure -M:run
```

## License

AGPL-3.0-or-later.
