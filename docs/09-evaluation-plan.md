# DocuMind Evaluation Plan

**Status:** Draft — evaluation not yet executed

**Phase:** Phase 0 — Design and Planning

## 1. Purpose

Evaluate whether DocuMind retrieves relevant document evidence, generates accurate answers grounded in that evidence, provides correct source citations, and abstains when the available evidence is insufficient.

## 2. Evaluation Dataset

### Initial dataset

- Target: 15–30 documents and 50–100 questions.

- Preferred initial source: synthetic documents created specifically for this project.

- Public documents may be added when their source and permitted use are documented.

- Do not use confidential, private, or real customer documents in the initial evaluation dataset.

### Dataset inventory

For each document, record:

- Document ID and filename.

- Document type and source.

- Whether it is synthetic or public.

- Relevant pages or sections.

- Expected facts and supporting evidence.

For each question, record:

- Question ID and question text.

- Related document IDs.

- Expected answer or expected abstention.

- Supporting page, section, or passage.

- Evaluation category.

- Human reviewer notes and final result.

## 3. Question Categories

The dataset should include:

- Answerable questions requiring a single fact.

- Questions requiring information from multiple passages.

- Questions requiring information from multiple documents.

- Questions with similar or potentially confusing evidence.

- Questions that cannot be answered from the available documents.

- Questions where retrieved evidence is insufficient to support a confident answer.

Target approximately 15–20% unanswerable questions, as specified in the Phase 0 plan.

## 4. Evaluation Metrics

### Answer correctness

Check whether the response answers the question accurately and agrees with the expected answer and source evidence.

### Citation correctness

Check whether each cited document and page or passage actually supports the associated claim.

### Abstention accuracy

Check whether DocuMind correctly declines to answer when the documents do not provide sufficient evidence.

### Retrieval relevance

Review whether the retrieved passages contain the evidence needed to answer the question.

### Latency

Measure the time taken to retrieve evidence and produce a response. Record the measurement method and test environment.

Numerical acceptance thresholds remain **TBD** until the initial baseline and project requirements are reviewed.

## 5. Evaluation Procedure

1\. Prepare the document collection and question-answer reference set.

2\. Record the dataset version and configuration.

3\. Run each question against the same documented system configuration.

4\. Save the generated answer, retrieved passages, citations, abstention decision, and latency.

5\. Have a reviewer compare the results with the expected answers and evidence.

6\. Record failures, categorize them, and investigate their causes.

7\. Repeat tests after relevant retrieval or generation changes.

8\. Compare results against the previous baseline before accepting a change.

## 6. Planned Experiments

Evaluate the effects of:

- Chunk size.

- Chunk overlap.

- Number of retrieved passages (top-k).

- Retrieval with and without reranking, when implemented.

Change one variable at a time where practical and record the configuration and results.

## 7. Results Log

For each evaluation run, record:

- Run ID and date.

- Dataset version.

- Model and configuration.

- Retrieval settings.

- Answer correctness results.

- Citation correctness results.

- Abstention results.

- Retrieval relevance results.

- Latency results.

- Known limitations and observations.

No evaluation results are claimed until the tests have actually been run.

## 8. Open Decisions

- Final dataset and source inventory.

- Numerical acceptance thresholds for each metric.

- Final embedding model and retrieval configuration.

- Evaluation environment and latency measurement method.

- Whether a second reviewer or automated grading will be used.

## 9. Exit Criteria

- [ ] Dataset source and permitted use documented.

- [ ] Initial target dataset size documented.

- [ ] Answerable and unanswerable question categories defined.

- [ ] Metrics and review procedure documented.

- [ ] Experiment and results-log formats defined.

- [ ] Acceptance thresholds reviewed and approved.

- [ ] Evaluation plan committed and pushed to GitHub.
