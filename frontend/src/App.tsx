import React, { useEffect, useRef, useState } from "react";
import {
  AlertTriangle,
  CheckCircle2,
  ChevronRight,
  ClipboardCheck,
  Database,
  Edit3,
  FileText,
  HelpCircle,
  LogOut,
  MessageSquare,
  Plus,
  Search,
  Send,
  ShieldCheck,
  Sparkles,
  ThumbsDown,
  ThumbsUp,
  Trash2,
  Upload,
  XCircle,
} from "lucide-react";
import { api, getToken, setToken } from "./api";
import type {
  ChatMessage,
  ChatSession,
  ComplianceChecklist,
  ComplianceRun,
  DocumentChunk,
  DocumentItem,
  ExtractionRun,
  SearchHit,
  Workspace,
} from "./types";

export default function App() {
  const [authed, setAuthed] = useState<boolean>(!!getToken());
  const [authMode, setAuthMode] = useState<"login" | "register">("login");
  const [email, setEmail] = useState("admin@documind.local");
  const [password, setPassword] = useState("Password123!");
  const [fullName, setFullName] = useState("Workspace Admin");
  const [authError, setAuthError] = useState<string | null>(null);
  const [authLoading, setAuthLoading] = useState(false);

  const [workspaces, setWorkspaces] = useState<Workspace[]>([]);
  const [activeWs, setActiveWs] = useState<Workspace | null>(null);
  const [documents, setDocuments] = useState<DocumentItem[]>([]);
  const [selectedDoc, setSelectedDoc] = useState<DocumentItem | null>(null);
  const [chunks, setChunks] = useState<DocumentChunk[]>([]);
  const [highlightedChunkId, setHighlightedChunkId] = useState<string | null>(null);

  const [activeTab, setActiveTab] = useState<"chat" | "extract" | "compliance" | "search">("chat");
  const [bannerError, setBannerError] = useState<string | null>(null);
  const [uploading, setUploading] = useState(false);

  // Semantic search state
  const [searchQuery, setSearchQuery] = useState("");
  const [searchHits, setSearchHits] = useState<SearchHit[]>([]);
  const [searching, setSearching] = useState(false);

  // Chat state
  const [sessions, setSessions] = useState<ChatSession[]>([]);
  const [activeSessionId, setActiveSessionId] = useState<string | null>(null);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [questionInput, setQuestionInput] = useState("");
  const [asking, setAsking] = useState(false);

  // Extraction state
  const [extractionRuns, setExtractionRuns] = useState<ExtractionRun[]>([]);
  const [schemaName, setSchemaName] = useState("enterprise_contract_v1");
  const [extracting, setExtracting] = useState(false);
  const [editingFieldId, setEditingFieldId] = useState<string | null>(null);
  const [editValueText, setEditValueText] = useState("");

  // Compliance state
  const [checklists, setChecklists] = useState<ComplianceChecklist[]>([]);
  const [selectedChecklistId, setSelectedChecklistId] = useState<string>("");
  const [complianceRuns, setComplianceRuns] = useState<ComplianceRun[]>([]);
  const [evaluating, setEvaluating] = useState(false);
  const [newChecklistName, setNewChecklistName] = useState("");
  const [newCriteriaText, setNewCriteriaText] = useState(
    "Monthly Uptime SLA | Vendor commits to a measurable monthly uptime percentage\nIP Indemnification Protection | Vendor indemnifies client against third-party IP infringement claims"
  );
  const [showNewChecklist, setShowNewChecklist] = useState(false);

  const chunkRefs = useRef<Record<string, HTMLDivElement | null>>({});

  async function handleAuthSubmit(e: React.FormEvent) {
    e.preventDefault();
    setAuthError(null);
    setAuthLoading(true);
    try {
      const res =
        authMode === "login"
          ? await api.login(email, password)
          : await api.register(fullName, email, password);
      setToken(res.accessToken);
      setAuthed(true);
    } catch (err) {
      setAuthError(err instanceof Error ? err.message : "Authentication failed");
    } finally {
      setAuthLoading(false);
    }
  }

  function handleLogout() {
    setToken(null);
    setAuthed(false);
    setWorkspaces([]);
    setActiveWs(null);
  }

  useEffect(() => {
    if (!authed) return;
    api
      .listWorkspaces()
      .then((wsList) => {
        setWorkspaces(wsList);
        if (wsList.length > 0) setActiveWs(wsList[0]);
      })
      .catch((err) => {
        if (String(err.message).includes("401")) handleLogout();
        else setBannerError(err.message);
      });
  }, [authed]);

  useEffect(() => {
    if (!activeWs) return;
    setBannerError(null);
    Promise.all([
      api.listDocuments(activeWs.id),
      api.listChatSessions(activeWs.id),
      api.listChecklists(activeWs.id),
    ])
      .then(async ([docs, sessList, clList]) => {
        setDocuments(docs);
        setSelectedDoc(docs[0] ?? null);
        setChecklists(clList);
        if (clList.length > 0) setSelectedChecklistId(clList[0].id);

        if (sessList.length > 0) {
          setSessions(sessList);
          setActiveSessionId(sessList[0].id);
        } else {
          const created = await api.createChatSession(activeWs.id, "Workspace Q&A");
          setSessions([created]);
          setActiveSessionId(created.id);
        }
      })
      .catch((err) => setBannerError(err.message));
  }, [activeWs]);

  useEffect(() => {
    if (!activeWs || !activeSessionId) return;
    api.listChatMessages(activeWs.id, activeSessionId).then(setMessages).catch(() => {});
  }, [activeWs, activeSessionId]);

  useEffect(() => {
    if (!activeWs || !selectedDoc) {
      setChunks([]);
      setExtractionRuns([]);
      setComplianceRuns([]);
      return;
    }
    Promise.all([
      api.listChunks(activeWs.id, selectedDoc.id),
      api.listExtractionRuns(activeWs.id, selectedDoc.id),
      api.listComplianceRuns(activeWs.id, selectedDoc.id),
    ])
      .then(([docChunks, extRuns, compRuns]) => {
        setChunks(docChunks);
        setExtractionRuns(extRuns);
        setComplianceRuns(compRuns);
      })
      .catch((err) => setBannerError(err.message));
  }, [activeWs, selectedDoc]);

  function jumpToChunk(docId: string, chunkId: string | null) {
    if (!chunkId) return;
    if (selectedDoc?.id !== docId) {
      const targetDoc = documents.find((d) => d.id === docId);
      if (targetDoc) setSelectedDoc(targetDoc);
    }
    setHighlightedChunkId(chunkId);
    setTimeout(() => {
      const el = chunkRefs.current[chunkId];
      if (el) el.scrollIntoView({ behavior: "smooth", block: "center" });
    }, 150);
  }

  async function handleFileUpload(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (!file || !activeWs) return;
    setUploading(true);
    setBannerError(null);
    try {
      const uploaded = await api.uploadDocument(activeWs.id, file);
      const updatedDocs = await api.listDocuments(activeWs.id);
      setDocuments(updatedDocs);
      setSelectedDoc(uploaded);
    } catch (err) {
      setBannerError(err instanceof Error ? err.message : "Upload failed");
    } finally {
      setUploading(false);
      e.target.value = "";
    }
  }

  async function handleDeleteDoc(docId: string) {
    if (!activeWs) return;
    try {
      await api.deleteDocument(activeWs.id, docId);
      const remaining = documents.filter((d) => d.id !== docId);
      setDocuments(remaining);
      if (selectedDoc?.id === docId) setSelectedDoc(remaining[0] ?? null);
    } catch (err) {
      setBannerError(err instanceof Error ? err.message : "Delete failed");
    }
  }

  async function handleSearch(e: React.FormEvent) {
    e.preventDefault();
    if (!activeWs || !searchQuery.trim()) return;
    setSearching(true);
    try {
      const res = await api.semanticSearch(activeWs.id, searchQuery.trim(), 5);
      setSearchHits(res.results);
    } catch (err) {
      setBannerError(err instanceof Error ? err.message : "Search failed");
    } finally {
      setSearching(false);
    }
  }

  async function handleAskQuestion(e: React.FormEvent) {
    e.preventDefault();
    if (!activeWs || !activeSessionId || !questionInput.trim()) return;
    const q = questionInput.trim();
    setQuestionInput("");
    setAsking(true);
    setBannerError(null);
    try {
      const turn = await api.askQuestion(activeWs.id, activeSessionId, q, 4);
      setMessages((prev) => [...prev, turn.userMessage, turn.assistantMessage]);
      const firstCite = turn.assistantMessage.citations?.[0];
      if (firstCite) jumpToChunk(firstCite.documentId, firstCite.chunkId);
    } catch (err) {
      setBannerError(err instanceof Error ? err.message : "Failed to answer question");
    } finally {
      setAsking(false);
    }
  }

  async function handleFeedback(messageId: string, fb: "HELPFUL" | "NOT_HELPFUL") {
    if (!activeWs) return;
    try {
      const updated = await api.submitFeedback(activeWs.id, messageId, fb);
      setMessages((prev) => prev.map((m) => (m.id === messageId ? updated : m)));
    } catch (err) {
      setBannerError(err instanceof Error ? err.message : "Feedback failed");
    }
  }

  async function handleRunExtraction() {
    if (!activeWs || !selectedDoc) return;
    setExtracting(true);
    setBannerError(null);
    try {
      const run = await api.runExtraction(activeWs.id, selectedDoc.id, schemaName, []);
      setExtractionRuns((prev) => [run, ...prev]);
    } catch (err) {
      setBannerError(err instanceof Error ? err.message : "Extraction failed");
    } finally {
      setExtracting(false);
    }
  }

  async function handleFieldReview(
    fieldId: string,
    status: "APPROVED" | "REJECTED" | "EDITED",
    overrideVal?: unknown
  ) {
    if (!activeWs) return;
    try {
      const updated = await api.reviewField(activeWs.id, fieldId, status, overrideVal);
      setExtractionRuns((prev) =>
        prev.map((run) => ({
          ...run,
          fields: run.fields.map((f) => (f.id === fieldId ? updated : f)),
        }))
      );
      setEditingFieldId(null);
    } catch (err) {
      setBannerError(err instanceof Error ? err.message : "Review update failed");
    }
  }

  async function handleCreateChecklist(e: React.FormEvent) {
    e.preventDefault();
    if (!activeWs || !newChecklistName.trim()) return;
    const items = newCriteriaText
      .split("\n")
      .map((line) => line.trim())
      .filter(Boolean)
      .map((line) => {
        const [criterion, description] = line.split("|").map((s) => s.trim());
        return { criterion, description: description || criterion };
      });
    if (items.length === 0) return;

    try {
      const created = await api.createChecklist(activeWs.id, newChecklistName.trim(), "", items);
      setChecklists((prev) => [created, ...prev]);
      setSelectedChecklistId(created.id);
      setNewChecklistName("");
      setShowNewChecklist(false);
    } catch (err) {
      setBannerError(err instanceof Error ? err.message : "Failed to create checklist");
    }
  }

  async function handleRunCompliance() {
    if (!activeWs || !selectedDoc || !selectedChecklistId) return;
    setEvaluating(true);
    setBannerError(null);
    try {
      const run = await api.runCompliance(activeWs.id, selectedDoc.id, selectedChecklistId);
      setComplianceRuns((prev) => [run, ...prev]);
    } catch (err) {
      setBannerError(err instanceof Error ? err.message : "Compliance evaluation failed");
    } finally {
      setEvaluating(false);
    }
  }

  async function handleComplianceOverride(resultId: string, outcome: "PASS" | "FAIL" | "UNCLEAR") {
    if (!activeWs) return;
    try {
      const updated = await api.reviewComplianceResult(
        activeWs.id,
        resultId,
        outcome,
        `Reviewer set outcome to ${outcome}`
      );
      setComplianceRuns((prev) =>
        prev.map((run) => ({
          ...run,
          results: run.results.map((r) => (r.id === resultId ? updated : r)),
        }))
      );
    } catch (err) {
      setBannerError(err instanceof Error ? err.message : "Override failed");
    }
  }

  if (!authed) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-slate-950 p-4">
        <div className="w-full max-w-md rounded-2xl border border-slate-800 bg-slate-900/90 p-8 shadow-2xl">
          <div className="flex items-center gap-3 mb-6">
            <div className="p-2.5 rounded-xl bg-indigo-600/20 border border-indigo-500/30 text-indigo-400">
              <ShieldCheck className="w-6 h-6" />
            </div>
            <div>
              <h1 className="text-xl font-bold tracking-tight text-white">DocuMind</h1>
              <p className="text-xs text-slate-400">Enterprise Document Intelligence Platform</p>
            </div>
          </div>

          <div className="flex rounded-lg bg-slate-950 p-1 mb-6 border border-slate-800">
            <button
              type="button"
              onClick={() => setAuthMode("login")}
              className={`flex-1 py-1.5 text-sm font-medium rounded-md transition ${
                authMode === "login" ? "bg-indigo-600 text-white" : "text-slate-400 hover:text-white"
              }`}
            >
              Sign In
            </button>
            <button
              type="button"
              onClick={() => setAuthMode("register")}
              className={`flex-1 py-1.5 text-sm font-medium rounded-md transition ${
                authMode === "register" ? "bg-indigo-600 text-white" : "text-slate-400 hover:text-white"
              }`}
            >
              Register
            </button>
          </div>

          {authError && (
            <div className="mb-4 rounded-lg border border-rose-500/30 bg-rose-950/50 px-3 py-2 text-xs text-rose-300">
              {authError}
            </div>
          )}

          <form onSubmit={handleAuthSubmit} className="space-y-4">
            {authMode === "register" && (
              <div>
                <label className="block text-xs font-medium text-slate-300 mb-1">Full Name</label>
                <input
                  type="text"
                  required
                  value={fullName}
                  onChange={(e) => setFullName(e.target.value)}
                  className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-white focus:border-indigo-500 focus:outline-none"
                />
              </div>
            )}
            <div>
              <label className="block text-xs font-medium text-slate-300 mb-1">Email</label>
              <input
                type="email"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-white focus:border-indigo-500 focus:outline-none"
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-slate-300 mb-1">Password</label>
              <input
                type="password"
                required
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-white focus:border-indigo-500 focus:outline-none"
              />
            </div>
            <button
              type="submit"
              disabled={authLoading}
              className="w-full rounded-lg bg-indigo-600 hover:bg-indigo-500 disabled:opacity-50 py-2.5 text-sm font-semibold text-white transition"
            >
              {authLoading ? "Authenticating..." : authMode === "login" ? "Sign In to Workspace" : "Create Account"}
            </button>
          </form>
        </div>
      </div>
    );
  }

  const latestExtraction = extractionRuns[0] ?? null;
  const latestCompliance = complianceRuns[0] ?? null;

  return (
    <div className="h-screen flex flex-col bg-slate-950 text-slate-100 overflow-hidden">
      {/* Top Navbar */}
      <header className="h-14 border-b border-slate-800 bg-slate-900/80 px-4 flex items-center justify-between shrink-0">
        <div className="flex items-center gap-4">
          <div className="flex items-center gap-2">
            <ShieldCheck className="w-5 h-5 text-indigo-400" />
            <span className="font-bold tracking-tight text-white">DocuMind</span>
          </div>
          <ChevronRight className="w-4 h-4 text-slate-600" />
          <select
            value={activeWs?.id ?? ""}
            onChange={(e) => {
              const found = workspaces.find((w) => w.id === e.target.value);
              if (found) setActiveWs(found);
            }}
            className="rounded-lg border border-slate-700 bg-slate-950 px-3 py-1.5 text-sm text-slate-200 focus:border-indigo-500 focus:outline-none"
          >
            {workspaces.map((ws) => (
              <option key={ws.id} value={ws.id}>
                {ws.name} ({ws.role})
              </option>
            ))}
          </select>
        </div>

        <button
          onClick={handleLogout}
          className="flex items-center gap-1.5 rounded-lg border border-slate-700 px-3 py-1.5 text-xs text-slate-300 hover:bg-slate-800 transition"
        >
          <LogOut className="w-3.5 h-3.5" />
          Sign Out
        </button>
      </header>

      {bannerError && (
        <div className="bg-rose-950/90 border-b border-rose-500/40 px-4 py-2 text-xs text-rose-200 flex items-center justify-between">
          <span>{bannerError}</span>
          <button onClick={() => setBannerError(null)} className="underline ml-4">
            Dismiss
          </button>
        </div>
      )}

      {/* Main 3-Column Studio */}
      <div className="flex-1 grid grid-cols-12 min-h-0">
        {/* Column 1: Document Sidebar (3 cols) */}
        <aside className="col-span-3 border-r border-slate-800 bg-slate-900/40 flex flex-col min-h-0">
          <div className="p-3 border-b border-slate-800 flex items-center justify-between">
            <span className="text-xs font-semibold uppercase tracking-wider text-slate-400">
              Documents ({documents.length})
            </span>
            <label className="cursor-pointer inline-flex items-center gap-1.5 rounded-lg bg-indigo-600 hover:bg-indigo-500 px-2.5 py-1.5 text-xs font-medium text-white transition">
              <Upload className="w-3.5 h-3.5" />
              {uploading ? "Ingesting..." : "Upload"}
              <input
                type="file"
                accept=".pdf,.docx,.txt,.md"
                onChange={handleFileUpload}
                disabled={uploading}
                className="hidden"
              />
            </label>
          </div>

          <div className="flex-1 overflow-y-auto p-2 space-y-1.5">
            {documents.length === 0 ? (
              <div className="p-6 text-center text-xs text-slate-500">
                Upload a PDF, DOCX, or TXT file to start indexing and querying.
              </div>
            ) : (
              documents.map((doc) => {
                const active = selectedDoc?.id === doc.id;
                return (
                  <div
                    key={doc.id}
                    onClick={() => setSelectedDoc(doc)}
                    className={`group rounded-xl p-2.5 cursor-pointer border transition ${
                      active
                        ? "bg-indigo-600/15 border-indigo-500/50 text-white"
                        : "bg-slate-900/60 border-slate-800/80 text-slate-300 hover:border-slate-700"
                    }`}
                  >
                    <div className="flex items-start justify-between gap-2">
                      <div className="flex items-center gap-2 min-w-0">
                        <FileText className="w-4 h-4 text-indigo-400 shrink-0" />
                        <span className="text-xs font-medium truncate">{doc.filename}</span>
                      </div>
                      <button
                        onClick={(ev) => {
                          ev.stopPropagation();
                          handleDeleteDoc(doc.id);
                        }}
                        className="opacity-0 group-hover:opacity-100 text-slate-500 hover:text-rose-400 transition"
                        title="Delete document"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                      </button>
                    </div>
                    <div className="mt-1.5 flex items-center justify-between text-[11px] text-slate-400">
                      <span>{doc.pageCount ?? 1} page(s)</span>
                      <span
                        className={`px-1.5 py-0.5 rounded font-medium ${
                          doc.status === "READY"
                            ? "bg-emerald-500/15 text-emerald-400"
                            : "bg-amber-500/15 text-amber-300"
                        }`}
                      >
                        {doc.status}
                      </span>
                    </div>
                  </div>
                );
              })
            )}
          </div>
        </aside>

        {/* Column 2: Evidence & Page-Chunk Viewer (4 cols) */}
        <section className="col-span-4 border-r border-slate-800 bg-slate-950 flex flex-col min-h-0">
          <div className="p-3 border-b border-slate-800 flex items-center justify-between">
            <div className="min-w-0">
              <h2 className="text-xs font-semibold uppercase tracking-wider text-slate-400">
                Source Evidence Viewer
              </h2>
              <p className="text-xs text-slate-300 truncate mt-0.5">
                {selectedDoc ? `${selectedDoc.filename} (${chunks.length} indexed chunks)` : "No document selected"}
              </p>
            </div>
            {highlightedChunkId && (
              <button
                onClick={() => setHighlightedChunkId(null)}
                className="text-[11px] text-indigo-400 hover:underline"
              >
                Clear highlight
              </button>
            )}
          </div>

          <div className="flex-1 overflow-y-auto p-3 space-y-3">
            {chunks.length === 0 ? (
              <div className="h-full flex items-center justify-center text-xs text-slate-500">
                Select an ingested document to inspect its page-aware passages.
              </div>
            ) : (
              chunks.map((c) => {
                const isHighlighted = highlightedChunkId === c.id;
                return (
                  <div
                    key={c.id}
                    ref={(el) => {
                      chunkRefs.current[c.id] = el;
                    }}
                    className={`rounded-xl border p-3 text-xs leading-relaxed transition ${
                      isHighlighted
                        ? "border-amber-400 bg-amber-500/10 ring-2 ring-amber-400/30"
                        : "border-slate-800 bg-slate-900/50 text-slate-300"
                    }`}
                  >
                    <div className="flex items-center justify-between text-[11px] text-slate-400 mb-1.5">
                      <span className="font-semibold text-indigo-300">
                        Chunk #{c.chunkIndex + 1} · Page {c.pageNumber ?? c.pageStart ?? 1}
                      </span>
                      <span>{c.tokenCount} tokens</span>
                    </div>
                    <p className="whitespace-pre-wrap text-slate-200">{c.content}</p>
                  </div>
                );
              })
            )}
          </div>
        </section>

        {/* Column 3: Intelligence Workbench (5 cols) */}
        <section className="col-span-5 bg-slate-900/30 flex flex-col min-h-0">
          {/* Workbench Tabs */}
          <div className="border-b border-slate-800 px-3 pt-2 flex gap-1 shrink-0">
            {[
              { id: "chat", label: "Grounded Q&A", icon: MessageSquare },
              { id: "extract", label: "Field Extraction", icon: Database },
              { id: "compliance", label: "Compliance", icon: ClipboardCheck },
              { id: "search", label: "Vector Search", icon: Search },
            ].map((tab) => {
              const Icon = tab.icon;
              const active = activeTab === tab.id;
              return (
                <button
                  key={tab.id}
                  onClick={() => setActiveTab(tab.id as typeof activeTab)}
                  className={`flex items-center gap-1.5 px-3 py-2 text-xs font-medium border-b-2 transition ${
                    active
                      ? "border-indigo-500 text-white"
                      : "border-transparent text-slate-400 hover:text-slate-200"
                  }`}
                >
                  <Icon className="w-3.5 h-3.5" />
                  {tab.label}
                </button>
              );
            })}
          </div>

          {/* TAB 1: Grounded Q&A Chat */}
          {activeTab === "chat" && (
            <div className="flex-1 flex flex-col min-h-0">
              <div className="flex-1 overflow-y-auto p-4 space-y-4">
                {messages.length === 0 && (
                  <div className="rounded-xl border border-slate-800 bg-slate-900/60 p-4 text-xs text-slate-400">
                    Ask any question across your workspace documents. Every claim includes clickable source citations, and out-of-scope questions trigger strict evidence refusal.
                  </div>
                )}

                {messages.map((m) => (
                  <div
                    key={m.id}
                    className={`rounded-2xl p-3.5 text-xs ${
                      m.role === "USER"
                        ? "bg-indigo-600/20 border border-indigo-500/30 ml-8 text-slate-100"
                        : "bg-slate-900 border border-slate-800 mr-4 text-slate-200"
                    }`}
                  >
                    <div className="flex items-center justify-between mb-1.5">
                      <span className="font-semibold text-[11px] uppercase tracking-wider text-slate-400">
                        {m.role === "USER" ? "You" : `DocuMind (${m.modelProvider ?? "grounded"})`}
                      </span>
                      {m.role === "ASSISTANT" && m.foundAnswer === false && (
                        <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full bg-amber-500/15 text-amber-300 text-[10px] font-medium">
                          <AlertTriangle className="w-3 h-3" /> Insufficient Evidence
                        </span>
                      )}
                    </div>

                    <p className="leading-relaxed whitespace-pre-wrap">{m.content}</p>

                    {m.citations && m.citations.length > 0 && (
                      <div className="mt-3 pt-2.5 border-t border-slate-800 space-y-1.5">
                        <div className="text-[10px] font-semibold uppercase tracking-wider text-slate-400">
                          Verified Sources (Click to inspect passage)
                        </div>
                        {m.citations.map((cite, idx) => (
                          <button
                            key={cite.id}
                            onClick={() => jumpToChunk(cite.documentId, cite.chunkId)}
                            className="w-full text-left rounded-lg border border-slate-700/80 bg-slate-950/80 hover:border-amber-400/60 p-2 transition"
                          >
                            <div className="flex items-center justify-between text-[11px] font-medium text-amber-300">
                              <span>
                                [S{idx + 1}] {cite.passageReference}
                              </span>
                              <span>{(cite.relevanceScore * 100).toFixed(1)}% match</span>
                            </div>
                            <p className="text-[11px] text-slate-400 line-clamp-2 mt-0.5">
                              "{cite.quotedText}"
                            </p>
                          </button>
                        ))}
                      </div>
                    )}

                    {m.role === "ASSISTANT" && (
                      <div className="mt-2.5 flex items-center gap-2">
                        <button
                          onClick={() => handleFeedback(m.id, "HELPFUL")}
                          className={`p-1 rounded hover:bg-slate-800 ${
                            m.feedback === "HELPFUL" ? "text-emerald-400" : "text-slate-500"
                          }`}
                          title="Helpful answer"
                        >
                          <ThumbsUp className="w-3.5 h-3.5" />
                        </button>
                        <button
                          onClick={() => handleFeedback(m.id, "NOT_HELPFUL")}
                          className={`p-1 rounded hover:bg-slate-800 ${
                            m.feedback === "NOT_HELPFUL" ? "text-rose-400" : "text-slate-500"
                          }`}
                          title="Not helpful"
                        >
                          <ThumbsDown className="w-3.5 h-3.5" />
                        </button>
                      </div>
                    )}
                  </div>
                ))}
              </div>

              <form onSubmit={handleAskQuestion} className="p-3 border-t border-slate-800 flex gap-2">
                <input
                  type="text"
                  value={questionInput}
                  onChange={(e) => setQuestionInput(e.target.value)}
                  placeholder="Ask a grounded question about workspace documents..."
                  disabled={asking}
                  className="flex-1 rounded-xl border border-slate-700 bg-slate-950 px-3 py-2 text-xs text-white focus:border-indigo-500 focus:outline-none"
                />
                <button
                  type="submit"
                  disabled={asking || !questionInput.trim()}
                  className="rounded-xl bg-indigo-600 hover:bg-indigo-500 disabled:opacity-50 px-3.5 py-2 text-xs font-semibold text-white flex items-center gap-1.5"
                >
                  <Send className="w-3.5 h-3.5" />
                  {asking ? "Thinking..." : "Ask"}
                </button>
              </form>
            </div>
          )}

          {/* TAB 2: Structured Field Extraction */}
          {activeTab === "extract" && (
            <div className="flex-1 overflow-y-auto p-4 space-y-4">
              <div className="rounded-xl border border-slate-800 bg-slate-900/70 p-3 space-y-2.5">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-semibold text-white">Run Schema Extraction</span>
                  <span className="text-[11px] text-slate-400">{selectedDoc?.filename ?? "Select a doc"}</span>
                </div>
                <div className="flex gap-2">
                  <input
                    type="text"
                    value={schemaName}
                    onChange={(e) => setSchemaName(e.target.value)}
                    className="flex-1 rounded-lg border border-slate-700 bg-slate-950 px-2.5 py-1.5 text-xs text-white"
                    placeholder="Schema name (e.g. enterprise_contract_v1)"
                  />
                  <button
                    onClick={handleRunExtraction}
                    disabled={extracting || !selectedDoc}
                    className="rounded-lg bg-indigo-600 hover:bg-indigo-500 disabled:opacity-50 px-3 py-1.5 text-xs font-semibold text-white flex items-center gap-1.5"
                  >
                    <Sparkles className="w-3.5 h-3.5" />
                    {extracting ? "Extracting..." : "Extract Fields"}
                  </button>
                </div>
              </div>

              {latestExtraction ? (
                <div className="space-y-2.5">
                  <div className="flex items-center justify-between text-[11px] text-slate-400">
                    <span>
                      Schema: <strong className="text-slate-200">{latestExtraction.schemaName}</strong>
                    </span>
                    <span>Provider: {latestExtraction.modelProvider}</span>
                  </div>

                  {latestExtraction.fields.map((field) => (
                    <div
                      key={field.id}
                      className="rounded-xl border border-slate-800 bg-slate-900/60 p-3 text-xs space-y-2"
                    >
                      <div className="flex items-center justify-between">
                        <span className="font-mono font-semibold text-indigo-300">{field.fieldName}</span>
                        <div className="flex items-center gap-2">
                          {field.confidence !== null && (
                            <span className="text-[11px] text-slate-400">
                              {(Number(field.confidence) * 100).toFixed(0)}% conf
                            </span>
                          )}
                          <span
                            className={`px-2 py-0.5 rounded-full text-[10px] font-semibold ${
                              field.reviewStatus === "APPROVED"
                                ? "bg-emerald-500/20 text-emerald-300"
                                : field.reviewStatus === "REJECTED"
                                ? "bg-rose-500/20 text-rose-300"
                                : field.reviewStatus === "EDITED"
                                ? "bg-sky-500/20 text-sky-300"
                                : "bg-slate-800 text-slate-300"
                            }`}
                          >
                            {field.reviewStatus}
                          </span>
                        </div>
                      </div>

                      {editingFieldId === field.id ? (
                        <div className="flex gap-2">
                          <input
                            type="text"
                            value={editValueText}
                            onChange={(e) => setEditValueText(e.target.value)}
                            className="flex-1 rounded border border-slate-700 bg-slate-950 px-2 py-1 text-xs text-white"
                          />
                          <button
                            onClick={() => handleFieldReview(field.id, "EDITED", editValueText)}
                            className="rounded bg-sky-600 px-2.5 py-1 text-[11px] font-medium text-white"
                          >
                            Save
                          </button>
                          <button
                            onClick={() => setEditingFieldId(null)}
                            className="rounded bg-slate-800 px-2 py-1 text-[11px] text-slate-300"
                          >
                            Cancel
                          </button>
                        </div>
                      ) : (
                        <div className="font-medium text-white">
                          {field.fieldValue !== null && field.fieldValue !== undefined
                            ? String(field.fieldValue)
                            : <span className="italic text-slate-500">Not found in document</span>}
                        </div>
                      )}

                      {field.evidence && (
                        <button
                          onClick={() => selectedDoc && jumpToChunk(selectedDoc.id, field.sourceChunkId)}
                          className="block text-left text-[11px] text-amber-300/90 hover:underline"
                        >
                          Evidence: "{field.evidence}"
                        </button>
                      )}

                      <div className="pt-1 flex items-center gap-2">
                        <button
                          onClick={() => handleFieldReview(field.id, "APPROVED")}
                          className="inline-flex items-center gap-1 rounded bg-emerald-600/20 hover:bg-emerald-600/30 border border-emerald-500/30 px-2 py-1 text-[11px] text-emerald-300"
                        >
                          <CheckCircle2 className="w-3 h-3" /> Approve
                        </button>
                        <button
                          onClick={() => {
                            setEditingFieldId(field.id);
                            setEditValueText(field.fieldValue ? String(field.fieldValue) : "");
                          }}
                          className="inline-flex items-center gap-1 rounded bg-sky-600/20 hover:bg-sky-600/30 border border-sky-500/30 px-2 py-1 text-[11px] text-sky-300"
                        >
                          <Edit3 className="w-3 h-3" /> Override
                        </button>
                        <button
                          onClick={() => handleFieldReview(field.id, "REJECTED")}
                          className="inline-flex items-center gap-1 rounded bg-rose-600/20 hover:bg-rose-600/30 border border-rose-500/30 px-2 py-1 text-[11px] text-rose-300"
                        >
                          <XCircle className="w-3 h-3" /> Reject
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              ) : (
                <div className="text-center text-xs text-slate-500 py-8">
                  Click "Extract Fields" to extract structured contract attributes with confidence scores.
                </div>
              )}
            </div>
          )}

          {/* TAB 3: Compliance Checklists */}
          {activeTab === "compliance" && (
            <div className="flex-1 overflow-y-auto p-4 space-y-4">
              <div className="rounded-xl border border-slate-800 bg-slate-900/70 p-3 space-y-2.5">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-semibold text-white">Compliance Checklist Evaluation</span>
                  <button
                    onClick={() => setShowNewChecklist((v) => !v)}
                    className="inline-flex items-center gap-1 text-[11px] text-indigo-400 hover:underline"
                  >
                    <Plus className="w-3 h-3" /> New Checklist
                  </button>
                </div>

                {showNewChecklist && (
                  <form onSubmit={handleCreateChecklist} className="space-y-2 border-t border-slate-800 pt-2">
                    <input
                      type="text"
                      required
                      value={newChecklistName}
                      onChange={(e) => setNewChecklistName(e.target.value)}
                      placeholder="Checklist Name (e.g. SOC2 Vendor Review)"
                      className="w-full rounded border border-slate-700 bg-slate-950 px-2.5 py-1.5 text-xs text-white"
                    />
                    <textarea
                      rows={3}
                      value={newCriteriaText}
                      onChange={(e) => setNewCriteriaText(e.target.value)}
                      placeholder="Criterion | Description (one per line)"
                      className="w-full rounded border border-slate-700 bg-slate-950 px-2.5 py-1.5 text-xs text-white"
                    />
                    <button
                      type="submit"
                      className="rounded bg-indigo-600 px-3 py-1.5 text-xs font-semibold text-white"
                    >
                      Save Checklist
                    </button>
                  </form>
                )}

                <div className="flex gap-2">
                  <select
                    value={selectedChecklistId}
                    onChange={(e) => setSelectedChecklistId(e.target.value)}
                    className="flex-1 rounded-lg border border-slate-700 bg-slate-950 px-2.5 py-1.5 text-xs text-white"
                  >
                    {checklists.length === 0 && <option value="">No checklists yet — create one above</option>}
                    {checklists.map((cl) => (
                      <option key={cl.id} value={cl.id}>
                        {cl.name} ({cl.items.length} rules)
                      </option>
                    ))}
                  </select>
                  <button
                    onClick={handleRunCompliance}
                    disabled={evaluating || !selectedDoc || !selectedChecklistId}
                    className="rounded-lg bg-indigo-600 hover:bg-indigo-500 disabled:opacity-50 px-3 py-1.5 text-xs font-semibold text-white"
                  >
                    {evaluating ? "Evaluating..." : "Run Audit"}
                  </button>
                </div>
              </div>

              {latestCompliance ? (
                <div className="space-y-2.5">
                  {latestCompliance.results.map((res) => (
                    <div
                      key={res.id}
                      className="rounded-xl border border-slate-800 bg-slate-900/60 p-3 text-xs space-y-2"
                    >
                      <div className="flex items-center justify-between gap-2">
                        <span className="font-semibold text-white">{res.criterion}</span>
                        <span
                          className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-bold ${
                            res.outcome === "PASS"
                              ? "bg-emerald-500/20 text-emerald-300"
                              : res.outcome === "FAIL"
                              ? "bg-rose-500/20 text-rose-300"
                              : "bg-amber-500/20 text-amber-300"
                          }`}
                        >
                          {res.outcome === "PASS" && <CheckCircle2 className="w-3 h-3" />}
                          {res.outcome === "FAIL" && <XCircle className="w-3 h-3" />}
                          {res.outcome === "UNCLEAR" && <HelpCircle className="w-3 h-3" />}
                          {res.outcome}
                        </span>
                      </div>

                      {res.explanation && <p className="text-slate-300">{res.explanation}</p>}

                      {res.evidence && (
                        <button
                          onClick={() => selectedDoc && jumpToChunk(selectedDoc.id, res.sourceChunkId)}
                          className="block text-left text-[11px] text-amber-300/90 hover:underline"
                        >
                          Supporting Passage: "{res.evidence}"
                        </button>
                      )}

                      <div className="pt-1 flex items-center gap-1.5">
                        <span className="text-[10px] text-slate-500 mr-1">Reviewer Override:</span>
                        {(["PASS", "FAIL", "UNCLEAR"] as const).map((status) => (
                          <button
                            key={status}
                            onClick={() => handleComplianceOverride(res.id, status)}
                            className={`px-2 py-0.5 rounded text-[10px] border transition ${
                              res.outcome === status
                                ? "border-indigo-500 bg-indigo-600/20 text-white"
                                : "border-slate-700 text-slate-400 hover:text-white"
                            }`}
                          >
                            {status}
                          </button>
                        ))}
                      </div>
                    </div>
                  ))}
                </div>
              ) : (
                <div className="text-center text-xs text-slate-500 py-8">
                  Select a checklist and click "Run Audit" to evaluate compliance rules against the document.
                </div>
              )}
            </div>
          )}

          {/* TAB 4: Semantic Vector Search */}
          {activeTab === "search" && (
            <div className="flex-1 overflow-y-auto p-4 space-y-4">
              <form onSubmit={handleSearch} className="flex gap-2">
                <input
                  type="text"
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  placeholder="Semantic search across all workspace documents..."
                  className="flex-1 rounded-xl border border-slate-700 bg-slate-950 px-3 py-2 text-xs text-white focus:border-indigo-500 focus:outline-none"
                />
                <button
                  type="submit"
                  disabled={searching || !searchQuery.trim()}
                  className="rounded-xl bg-indigo-600 hover:bg-indigo-500 disabled:opacity-50 px-3.5 py-2 text-xs font-semibold text-white"
                >
                  {searching ? "Searching..." : "Search"}
                </button>
              </form>

              <div className="space-y-2.5">
                {searchHits.map((hit) => (
                  <button
                    key={hit.chunkId}
                    onClick={() => jumpToChunk(hit.documentId, hit.chunkId)}
                    className="w-full text-left rounded-xl border border-slate-800 bg-slate-900/60 hover:border-indigo-500/50 p-3 text-xs transition"
                  >
                    <div className="flex items-center justify-between text-[11px] font-semibold text-indigo-300 mb-1">
                      <span>
                        {hit.documentFilename} · Page {hit.pageNumber ?? hit.pageStart ?? 1}
                      </span>
                      <span className="text-emerald-400">{(hit.similarityScore * 100).toFixed(1)}% cosine</span>
                    </div>
                    <p className="text-slate-300 line-clamp-3">{hit.content}</p>
                  </button>
                ))}
              </div>
            </div>
          )}
        </section>
      </div>
    </div>
  );
}
