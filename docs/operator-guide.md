# Operator Guide

This guide is for lab operators and technology partners deploying the
chemical engineering technician lab test and material analysis actor.

## Operational workflow

### 1. Batch registration

Before the actor can handle any operations, the material batch must be
registered in the store:

```clojure
(def store (store/mem-store
  {:batches {:batch-001 {:batch-id :batch-001
                         :name "Solvent Lot 42"
                         :material-type :solvent}}}))
```

The Governor requires the batch record to exist for any operation to
proceed.

### 2. Submit an operational request

A technician or a lab-equipment integration submits a request:

```clojure
(def request {:batch-id :batch-001
              :op :draft-lab-test
              :stake :low
              :payload {:test "purity-assay"
                        :result-pct 99.4}})
```

Allowed operations:
- `:draft-lab-test` — routine lab test data recording.
- `:record-material-analysis` — material analysis logging.
- `:flag-chemical-hazard` — surface a chemical-safety hazard; **ALWAYS
  escalates**.
- `:schedule-batch-review` — batch verification/review scheduling.

### 3. Run the actor

```clojure
(require '[chem.actor :as actor])

(def graph (actor/build-graph {:store store}))

(def result (actor/run-request! graph request {} "thread-001"))
;; Result: {:state {:record {...} :audit [...]}
;;          :events [...]
;;          :status :done|:interrupted
;;          :frontier ...}
```

**Possible outcomes:**

- **`:status :done`** (no escalation required)
  - The request succeeded and was committed to the store.
  - Check `:state :record` for the persisted record.

- **`:status :interrupted`** (waiting for human approval)
  - The proposal was flagged for escalation (either
    `:flag-chemical-hazard` or low advisor confidence).
  - A human technician/engineer must review the proposal and decide
    whether to approve or deny.
  - Use `actor/approve!` to resume (see step 4).

- **`:status :hold`** (rejected, no escalation)
  - The Governor rejected the proposal permanently (hard violation).
  - Examples: unregistered batch, non-`:propose` effect.
  - No recovery possible; the proposal is discarded.

### 4. Human approval (if interrupted)

If the actor returned `:status :interrupted`, a human must approve:

```clojure
(def approval-result (actor/approve! graph "thread-001"))
;; This resumes the interrupted request and commits it.
```

After approval, the record is committed and the actor returns
`:status :done`.

### 5. Audit & compliance

All decisions are logged in the audit ledger:

```clojure
(store/ledger store)
;; Returns: [{:node :advise :request {...} :proposal {...}}
;;           {:node :govern :verdict {...}}
;;           {:disposition :request-approval ...}
;;           {:node :commit :record {...}}]
```

Export the ledger regularly for compliance audits and incident
investigation.

## Customizing the Advisor

The default `mock-advisor` is deterministic and suitable for testing. For
production:

1. Implement the `Advisor` protocol:
   ```clojure
   (deftype LLMAdvisor [model]
     Advisor
     (-advise [_ store request]
       ;; Call your LLM to propose an action.
       ;; Always return :effect :propose.
       ;; Return :confidence 0.0 on parse failure (forces escalation).
       ))
   ```

2. Pass it to `build-graph`:
   ```clojure
   (actor/build-graph
     {:store store
      :advisor (LLMAdvisor. your-model)})
   ```

## Customizing the Governor

The Governor policy is in `src/chem/governor.cljc`. If your lab has
different safety rules:

1. Modify the `:escalate?` logic (e.g., additional ops that require human
   approval).
2. Modify the `:hard?` violations (e.g., additional prerequisites that
   always reject).
3. Adjust `confidence-floor` if your advisor has different reliability
   metrics.
4. Document why the change is needed in a comment or ADR.

**Important:** Do not weaken the hard invariants:
- `:no-batch` — always reject unregistered batches.
- `:no-actuation` — always require `:effect :propose`.

## Integration with external systems

### Lab equipment / LIMS ingestion
Your LIMS or analytical instrument submits requests to the actor:
```clojure
(actor/run-request! graph
  {:batch-id :batch-001
   :op :record-material-analysis
   :payload {:test "purity-assay" ...}}
  {}
  "lims-thread-001")
```

### QA / batch review
When the actor commits a `:schedule-batch-review` proposal, push it to
your QA system:
```clojure
(when (= :schedule-batch-review (:op (:record state)))
  (qa/create-review-task (:record state)))
```

### Alerting / escalation
When `:flag-chemical-hazard` is submitted, the actor escalates to human
approval:
```clojure
(if (= :interrupted (:status result))
  (send-alert-to-operator "Chemical hazard flagged, awaiting approval"))
```

## Troubleshooting

**Q: My request returned `:status :hold`. Why?**
A: The Governor rejected it as a hard violation. Check the `:verdict` in
the audit ledger for details. Common causes:
- Batch not registered.
- Proposal `:effect` is not `:propose`.

**Q: My request returned `:status :interrupted`. What do I do?**
A: A human technician/engineer must review the proposal and call
`actor/approve!` to resume. This is the intended flow for escalations
(chemical hazards, low-confidence advisors).

**Q: How do I export the audit ledger for compliance?**
A: Call `(store/ledger store)` and serialize to JSON or CSV. The ledger is
append-only and tamper-evident.

**Q: Can I integrate with an LLM?**
A: Yes. Implement the `Advisor` protocol and swap `mock-advisor` for your
LLM advisor. Always return `:confidence 0.0` on parse failures (forces
escalation, never fabricated confidence).

## Further reading

- [`README.md`](../README.md) — project overview and design rationale.
- [`src/chem/governor.cljc`](../src/chem/governor.cljc) — hard/escalation
  invariants.
- [`src/chem/actor.cljc`](../src/chem/actor.cljc) — StateGraph wiring and
  flow.
