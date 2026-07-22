# Business Model

## Positioning

Chemical engineering technicians run lab testing, material analysis, and
batch-quality workflows that are routinely tracked in spreadsheets, paper lab
notebooks, or proprietary closed SaaS platforms that lock in the lab and
expose it to vendor risk — an especially poor fit when chemical-hazard
findings and batch-documentation records need to be tamper-evident and
auditable.

This open-source blueprint decouples lab test and material analysis
coordination from vendor lock-in by providing a reference implementation of
an actor-based, governance-gated robotics system that a lab or plant operator
can deploy, modify, and own.

## Value proposition

**For a lab or plant operator:**
- Keep your own test records and material-analysis history (not rented from
  a SaaS vendor).
- Audit-logged, tamper-evident lab test and hazard-escalation decisions.
- Routine lab test data recording and analysis logging can be automated
  without loss of human oversight (escalation to a human for any chemical
  hazard).
- Integrated with your own identity and credential systems (no vendor API
  keys required).
- Forkable and modifiable to your specific batch-documentation and
  regulatory requirements.

**For a technology partner:**
- Deploy this as a reference implementation for your customers.
- Customize the Advisor (swap `mock-advisor` for your domain-specific LLM).
- Customize the Governor rules to reflect your lab's chemical-safety
  standards and regulatory constraints.
- Operate it on your own cloud or on-premises infrastructure.

## Economic model

This is **open-source software, AGPL-3.0-or-later**. There is no per-seat
licensing or per-operation fee. A lab operator or technology partner can:
1. Deploy this reference implementation (code provided).
2. Hire or retain engineers/technicians to operate it.
3. Customize the Advisor or Governor as needed for the lab's regulatory
   environment.
4. Participate in the community (issues, PRs, documentation improvements).

## Regulatory & Safety considerations

**This actor is NOT a chemical dispensing/dosing/reaction-control system.**
It does not:
- Dispense, mix, or dose chemicals.
- Control reactor or process conditions.
- Release or clear a batch for shipment.

It supports **lab test data recording, material analysis logging, and
chemical-hazard escalation only** — a lab test robot records readings and
findings for human review; all release/dosing/reaction decisions remain
under licensed technician/engineer control.

This scoping is intentional and enforced at the Governor level. It keeps the
system's attack surface small and makes it easy to certify that the actor
cannot inadvertently release an unsafe batch or suppress a hazard finding.

## Deployment architecture

### Minimal single-lab deployment
```
┌─────────────────────┐
│    Lab Operator      │
│   (human approval)   │
└──────────┬──────────┘
           │ (approval)
        ┌──▼──────────────────┐
        │    ChemActor         │
        │  (langgraph graph)  │
        └──┬───────────────────┘
           │
    ┌──────┴────────┬──────────────┐
    │               │              │
┌───▼────┐  ┌──────▼─────┐  ┌─────▼──┐
│ Advisor │  │  Governor  │  │ Store  │
│ (mock)  │  │  (policy)  │  │ (file) │
└─────────┘  └────────────┘  └────────┘
    │               │              │
    └───────────────┴──────────────┘
           (in-process)
```

### Scaled multi-batch deployment
```
┌─────────────────────────────────────────────┐
│   Operator Console (web, shared frontend)   │
│   (authentication, human approval UI)       │
└──────────┬──────────────────────────────────┘
           │
    ┌──────┴────────┬────────────┐
    │               │            │
┌───▼────────┐  ┌──▼────────┐ ┌─┴────────┐
│  Batch A   │  │  Batch B  │ │ Batch C  │
│  actor pod │  │ actor pod │ │actor pod │
└──────┬─────┘  └─────┬─────┘ └──┬──────┘
       │              │          │
   ┌───┴──────────────┴──────────┘
   │
┌──▼──────────────────────┐
│  Shared audit ledger    │
│  (durable backing)      │
└─────────────────────────┘
```

### Integration points
- **Lab equipment ingestion**: LIMS (Laboratory Information Management
  System), analytical instruments (GC, HPLC, spectrometers) → lab's
  infrastructure.
- **Advisor**: Can be swapped out for an LLM (langchain) or domain-specific
  model.
- **Identity**: Technician/engineer approvals linked to authenticated user
  identities (OAuth, OIDC, LDAP).
- **Audit ledger**: Can be durable (Postgres, DynamoDB) or ephemeral (for
  testing).
- **Downstream actions**: Approved batch-review scheduling → QA system, or
  email, or Slack notifications.

## Maturity & Roadmap

**Phase 1 (current):** Reference implementation with `mock-advisor`,
in-memory store, and manual testing.

**Phase 2 (future):** LLM advisor integration (langchain/claude), durable
audit ledger (Postgres), web-based operator console for approval UI.

**Phase 3 (future, if adopted):** Multi-batch deployment, LIMS integration
templates, QA system integrations, regulatory compliance packs (GLP, ISO
17025, etc.).
