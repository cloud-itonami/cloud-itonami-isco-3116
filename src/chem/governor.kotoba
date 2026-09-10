(ns chem.governor
  "ChemGovernor — the independent safety/traceability layer for
  the ISCO-08 3116 chemical engineering technician lab test and material analysis
  actor. Wired as its own `:govern` node in `chem.actor`'s StateGraph,
  downstream of `:advise` — the Advisor has no notion of batch provenance or
  chemical-hazard risk, so this MUST be a separate system able to
  reject a proposal (itonami actor pattern, per ADR-2607011000 / CLAUDE.md
  Actors section).

  `check` is a pure function of (request, context, proposal, store) ->
  verdict; it never mutates the store. The StateGraph's `:decide` node
  routes on the verdict:
    :hard? true                → :hold  (irreversible, no write)
    :escalate? true            → :request-approval (interrupt-before)
    otherwise                  → :commit

  HARD invariants (:hard? true, ALWAYS :hold, never overridable):
    1. batch provenance  — the request's batch must be registered.
    2. no-actuation         — proposal :effect must be :propose.
  ESCALATION invariants (:escalate? true, ALWAYS human sign-off, per the
  README robotics-premise: chemical hazards always require human sign-off):
    3. :op :flag-chemical-hazard.
    4. low confidence (< `confidence-floor`)."
  (:require [chem.store :as store]))

(def confidence-floor 0.6)
(def ^:private escalating-ops #{:flag-chemical-hazard})

(defn- hard-violations [{:keys [proposal]} batch-record]
  (cond-> []
    (nil? batch-record)
    (conj {:rule :no-batch :detail "未登録 batch"})

    (not= :propose (:effect proposal))
    (conj {:rule :no-actuation :detail "effect は :propose のみ許可（直接書込禁止）"})))

(defn check
  "Assess a proposal against `request`/`context`/`proposal` and a
  `store` implementing `chem.store/Store`. Returns
  `{:ok? bool :violations [...] :confidence n :hard? bool :escalate? bool}`."
  [request context proposal store]
  (let [batch-record (store/batch store (:batch-id request))
        hard (hard-violations {:proposal proposal} batch-record)
        hard? (boolean (seq hard))
        conf (or (:confidence proposal) 0.0)
        low? (< conf confidence-floor)
        risky-op? (contains? escalating-ops (:op proposal))]
    {:ok? (and (not hard?) (not low?) (not risky-op?))
     :violations hard
     :confidence conf
     :hard? hard?
     :escalate? (and (not hard?) (or low? risky-op?))}))
