import type {
  AuthResponse,
  ChatMessage,
  ChatSession,
  ComplianceChecklist,
  ComplianceResult,
  ComplianceRun,
  DocumentChunk,
  DocumentItem,
  ExtractedField,
  ExtractionRun,
  SearchResponse,
  Workspace,
} from "./types";

const TOKEN_KEY = "documind_access_token";

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}

export function setToken(token: string | null) {
  if (token) localStorage.setItem(TOKEN_KEY, token);
  else localStorage.removeItem(TOKEN_KEY);
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const token = getToken();
  const headers = new Headers(init.headers || {});
  if (token) headers.set("Authorization", `Bearer ${token}`);
  if (!(init.body instanceof FormData) && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }

  const res = await fetch(`/api/v1${path}`, { ...init, headers });
  if (!res.ok) {
    let msg = `Request failed (${res.status})`;
    try {
      const err = await res.json();
      msg = err.message || err.detail || err.error || msg;
    } catch {
      // ignore non-json body
    }
    throw new Error(msg);
  }
  if (res.status === 204) return undefined as T;
  return res.json() as Promise<T>;
}

export const api = {
  login: (email: string, password: string) =>
    request<AuthResponse>("/auth/login", {
      method: "POST",
      body: JSON.stringify({ email, password }),
    }),

  register: (fullName: string, email: string, password: string) =>
    request<AuthResponse>("/auth/register", {
      method: "POST",
      body: JSON.stringify({ fullName, email, password }),
    }),

  listWorkspaces: () => request<Workspace[]>("/workspaces"),

  createWorkspace: (name: string, description: string) =>
    request<Workspace>("/workspaces", {
      method: "POST",
      body: JSON.stringify({ name, description }),
    }),

  listDocuments: (wsId: string) => request<DocumentItem[]>(`/workspaces/${wsId}/documents`),

  uploadDocument: (wsId: string, file: File) => {
    const fd = new FormData();
    fd.append("file", file);
    return request<DocumentItem>(`/workspaces/${wsId}/documents`, {
      method: "POST",
      body: fd,
    });
  },

  deleteDocument: (wsId: string, docId: string) =>
    request<void>(`/workspaces/${wsId}/documents/${docId}`, { method: "DELETE" }),

  listChunks: (wsId: string, docId: string) =>
    request<DocumentChunk[]>(`/workspaces/${wsId}/documents/${docId}/chunks`),

  semanticSearch: (wsId: string, query: string, topK = 5) =>
    request<SearchResponse>(`/workspaces/${wsId}/search`, {
      method: "POST",
      body: JSON.stringify({ query, topK }),
    }),

  listChatSessions: (wsId: string) =>
    request<ChatSession[]>(`/workspaces/${wsId}/chat/sessions`),

  createChatSession: (wsId: string, title?: string) =>
    request<ChatSession>(`/workspaces/${wsId}/chat/sessions`, {
      method: "POST",
      body: JSON.stringify({ title }),
    }),

  listChatMessages: (wsId: string, sessionId: string) =>
    request<ChatMessage[]>(`/workspaces/${wsId}/chat/sessions/${sessionId}/messages`),

  askQuestion: (wsId: string, sessionId: string, question: string, topK = 4) =>
    request<{ userMessage: ChatMessage; assistantMessage: ChatMessage }>(
      `/workspaces/${wsId}/chat/sessions/${sessionId}/messages`,
      {
        method: "POST",
        body: JSON.stringify({ question, topK }),
      }
    ),

  submitFeedback: (wsId: string, messageId: string, feedback: "HELPFUL" | "NOT_HELPFUL") =>
    request<ChatMessage>(`/workspaces/${wsId}/chat/messages/${messageId}/feedback`, {
      method: "PATCH",
      body: JSON.stringify({ feedback }),
    }),

  listExtractionRuns: (wsId: string, docId: string) =>
    request<ExtractionRun[]>(`/workspaces/${wsId}/documents/${docId}/extractions`),

  runExtraction: (wsId: string, docId: string, schemaName: string, fields: { fieldName: string; description: string }[]) =>
    request<ExtractionRun>(`/workspaces/${wsId}/documents/${docId}/extractions`, {
      method: "POST",
      body: JSON.stringify({ schemaName, fields: fields.length ? fields : undefined }),
    }),

  reviewField: (wsId: string, fieldId: string, reviewStatus: "APPROVED" | "REJECTED" | "EDITED", overrideValue?: unknown) =>
    request<ExtractedField>(`/workspaces/${wsId}/extractions/fields/${fieldId}/review`, {
      method: "PATCH",
      body: JSON.stringify({ reviewStatus, overrideValue }),
    }),

  listChecklists: (wsId: string) =>
    request<ComplianceChecklist[]>(`/workspaces/${wsId}/checklists`),

  createChecklist: (wsId: string, name: string, description: string, items: { criterion: string; description: string }[]) =>
    request<ComplianceChecklist>(`/workspaces/${wsId}/checklists`, {
      method: "POST",
      body: JSON.stringify({ name, description, items }),
    }),

  listComplianceRuns: (wsId: string, docId: string) =>
    request<ComplianceRun[]>(`/workspaces/${wsId}/documents/${docId}/compliance-runs`),

  runCompliance: (wsId: string, docId: string, checklistId: string) =>
    request<ComplianceRun>(`/workspaces/${wsId}/documents/${docId}/compliance-runs`, {
      method: "POST",
      body: JSON.stringify({ checklistId }),
    }),

  reviewComplianceResult: (wsId: string, resultId: string, outcome: "PASS" | "FAIL" | "UNCLEAR", explanation?: string) =>
    request<ComplianceResult>(`/workspaces/${wsId}/compliance/results/${resultId}/review`, {
      method: "PATCH",
      body: JSON.stringify({ outcome, explanation }),
    }),
};
