# Contributing

`cloud-itonami-isic-4540` accepts contributions to the OSS blueprint,
capability bindings, policy tests, documentation and operator model.

## Development
The capability layer lives in `kotoba-lang/robotics` and
`kotoba-lang/langgraph`. This repo holds the business blueprint and
operator contracts.

```bash
clojure -M:test
clojure -M:lint
```

## Rules
- Do not commit real customer or payment data.
- Keep every commit path behind the MotorcycleOpsGovernor.
- Never add a proposal op, or relax the scope-exclusion check, in a
  way that lets any op directly finalize a roadworthiness-clearance
  decision. That authority must never enter this actor's closed op
  allowlist.
- Treat service-record/scheduling/safety-concern/parts-order
  workflows as high-risk: add tests for account-verification gating,
  scope exclusion, evidence, disclosure and audit logging.
- Document any new business-model or operator assumption in `docs/`.

## Pull Requests
PRs should describe: what behavior changed, which policy invariant is
affected, how it was tested, whether operator or certification docs need
updates.
