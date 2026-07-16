# Security Policy

This project handles sale/repair-order, service-scheduling and
safety-concern-flagging workflows for a motorcycle dealership/
workshop. Treat vulnerabilities as potentially high impact even when
the demo data is synthetic -- a bypass here has a direct road-safety
dimension.

## Do Not Disclose Publicly

Report privately before opening public issues for:

- a bypass of the roadworthiness-clearance scope exclusion
- real customer or payment data exposure
- authorization bypass
- MotorcycleOpsGovernor bypass
- audit-ledger tampering
- over-disclosure in service or reconciliation records/exports
- tenant isolation failures

## Reporting

Use GitHub private vulnerability reporting when available for the repository.
If that is unavailable, contact the repository maintainers through the
cloud-itonami organization before publishing details.

Include:

- affected commit or version
- reproduction steps
- expected and actual behavior
- impact on customer/payment data, policy enforcement or audit logging
- suggested fix, if known

## Production Guidance

- Store secrets outside Git.
- Keep real customer and payment data outside this repository.
- Run policy tests before deployment.
- Export and review audit logs regularly.
- Use least privilege for operators and service accounts.
