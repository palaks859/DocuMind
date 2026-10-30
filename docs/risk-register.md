# DocuMind Risk Register

**Status:** Draft — risks require ongoing review

**Phase:** Phase 0 — Design and Planning

## 1. Purpose

Identify project risks early, document mitigation strategies, and review risks throughout development.

## 2. Risk Register

| ID | Risk | Potential impact | Mitigation | Status |
|---|---|---|---|---|
| R-001 | LLM free-tier limits or provider changes | AI features may become unavailable or expensive | Use a provider-independent LLM interface; evaluate a second provider and a local-model option | Open |
| R-002 | Free hosting sleeps or performs slowly | Poor demo experience | Show clear loading states and prepare a demo video as a backup | Open |
| R-003 | Scope creep | MVP may not be completed on time | Freeze the agreed MVP scope; record new ideas in the future roadmap | Open |
| R-004 | Poor retrieval quality | Answers may miss relevant evidence | Build an evaluation dataset and run retrieval-tuning experiments; consider hybrid search later | Open |
| R-005 | Hallucinated or unsupported citations | Users may trust incorrect answers | Validate citations against retrieved document chunks and test citation correctness | Open |
| R-006 | Sensitive document data exposure | Privacy and security harm | Use public or synthetic data for initial evaluation; ensure document deletion covers associated chunks | Open |
| R-007 | Free-tool terms or limits change | Development workflow may be disrupted | Recheck relevant tool limits and terms at the start of each phase | Open |

## 3. Risk Review Procedure

1\. Review this register at the start of each project phase.

2\. Add newly identified risks and update existing entries.

3\. Record the owner, likelihood, severity, mitigation progress, and review date as the project develops.

4\. Escalate risks that could block the MVP or compromise security.

5\. Do not mark a risk resolved until the mitigation has been verified.

## 4. Open Decisions

- Risk likelihood and severity scoring method.

- Risk owners and review cadence.

- Backup hosting and LLM provider options.

- Document deletion and retention verification procedure.

## 5. Exit Criteria

- [ ] All initial risks and mitigations reviewed.

- [ ] Risk scoring method and review cadence agreed.

- [ ] Security and data-handling risks reviewed against the security design.

- [ ] Risk register committed and pushed to GitHub.
