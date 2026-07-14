(ns chem.store
  "SSoT for the ISCO-08 3116 chemical engineering technician lab test
  and material analysis actor. Store is a protocol injected into the `chem.actor`
  StateGraph — `MemStore` is the default, deterministic, zero-dep backend; a
  Datomic/kotoba-server-backed implementation can be swapped in without
  touching the actor or governor (itonami actor pattern, per
  ADR-2607011000 / CLAUDE.md Actors section).

  Domain:

    batch    — a registered material batch under quality control (:batch-id, :name, :material-type)
    record   — a committed lab test/analysis record under a batch
               (lab test data, material analysis log, batch verification note) —
               written ONLY via commit-record!, never mutated in place
    ledger   — an append-only audit trail of every proposal/verdict/
               disposition, regardless of outcome (commit or hold)")

(defprotocol Store
  (batch [s batch-id])
  (records-of [s batch-id])
  (ledger [s])
  (register-batch! [s batch])
  (commit-record! [s record])
  (append-ledger! [s fact]))

(defrecord MemStore [a]
  Store
  (batch [_ batch-id] (get-in @a [:batches batch-id]))
  (records-of [_ batch-id] (filter #(= batch-id (:batch-id %)) (:records @a)))
  (ledger [_] (:ledger @a))
  (register-batch! [s batch]
    (swap! a assoc-in [:batches (:batch-id batch)] batch) s)
  (commit-record! [s record]
    (swap! a update :records (fnil conj []) record) s)
  (append-ledger! [s fact]
    (swap! a update :ledger (fnil conj []) fact) s))

(defn mem-store
  ([] (mem-store {}))
  ([seed] (->MemStore (atom (merge {:batches {} :records [] :ledger []} seed)))))
