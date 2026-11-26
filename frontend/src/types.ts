export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresInSeconds: number;
  user: {
    id: string;
    email: string;
    fullName: string;
  };
}

export interface Workspace {
  id: string;
  name: string;
  slug: string;
  description: string | null;
  role: "ADMIN" | "REVIEWER" | "VIEWER";
  createdAt: string;
}

export interface DocumentItem {
  id: string;
  workspaceId: string;
  filename: string;
  contentType: string;
  sizeBytes: number;
  status: "UPLOADED" | "PROCESSING" | "READY" | "FAILED";
  pageCount: number | null;
  errorMessage: string | null;
  createdAt: string;
}

export interface DocumentChunk {
  id: string;
  documentId: string;
  chunkIndex: number;
  content: string;
  tokenCount: number;
  pageNumber: number | null;
  pageStart: number | null;
  pageEnd: number | null;
}

export interface SearchHit {
  chunkId: string;
  documentId: string;
  documentFilename: string;
  chunkIndex: number;
  pageNumber: number | null;
  pageStart: number | null;
  pageEnd: number | null;
  content: string;
  similarityScore: number;
}

export interface SearchResponse {
  query: string;
  topK: number;
  results: SearchHit[];
}

export interface Citation {
  id: string;
  chunkId: string;
  documentId: string;
  documentFilename: string;
  pageNumber: number | null;
  pageStart: number | null;
  pageEnd: number | null;
  passageReference: string;
  quotedText: string;
  relevanceScore: number;
}

export interface ChatMessage {
  id: string;
  sessionId: string;
  role: "USER" | "ASSISTANT";
  content: string;
  foundAnswer: boolean | null;
  modelProvider: string | null;
  modelName: string | null;
  feedback: "HELPFUL" | "NOT_HELPFUL" | null;
  createdAt: string;
  citations: Citation[];
}

export interface ChatSession {
  id: string;
  workspaceId: string;
  title: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface ExtractedField {
  id: string;
  extractionRunId: string;
  fieldName: string;
  fieldValue: unknown;
  sourceChunkId: string | null;
  evidence: string | null;
  pageNumber: number | null;
  pageStart: number | null;
  pageEnd: number | null;
  confidence: number | null;
  reviewStatus: "PENDING" | "APPROVED" | "REJECTED" | "EDITED";
  reviewedBy: string | null;
  reviewedAt: string | null;
}

export interface ExtractionRun {
  id: string;
  documentId: string;
  status: string;
  schemaName: string;
  promptVersion: string | null;
  modelProvider: string | null;
  modelName: string | null;
  createdAt: string;
  completedAt: string | null;
  fields: ExtractedField[];
}

export interface ChecklistItem {
  id: string;
  criterion: string;
  description: string | null;
  itemOrder: number;
}

export interface ComplianceChecklist {
  id: string;
  workspaceId: string;
  name: string;
  description: string | null;
  createdAt: string;
  items: ChecklistItem[];
}

export interface ComplianceResult {
  id: string;
  checklistItemId: string;
  criterion: string;
  sourceChunkId: string | null;
  outcome: "PASS" | "FAIL" | "UNCLEAR";
  evidence: string | null;
  explanation: string | null;
  reviewedBy: string | null;
}

export interface ComplianceRun {
  id: string;
  documentId: string;
  checklistId: string;
  status: string;
  promptVersion: string | null;
  modelProvider: string | null;
  modelName: string | null;
  createdAt: string;
  completedAt: string | null;
  results: ComplianceResult[];
}
