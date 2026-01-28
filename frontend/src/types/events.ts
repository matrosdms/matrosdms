export interface InboxFile {
  /** 
   * SHA-256 Content Hash. 
   * This is the unique identifier during the Staging/Inbox phase.
   */
  sha256: string; 
  
  // Current State
  status: 'PROCESSING' | 'READY' | 'ERROR' | 'DUPLICATE';
  source?: 'UPLOAD' | 'EMAIL' | 'SCAN' | 'API';
  progressMessage?: string; // "Queued..."
  
  // 1. Technical Info
  fileInfo: {
    originalFilename: string; // e.g. "scan_2024.eml"
    sizeBytes?: number;
    contentType?: string;     // e.g. "message/rfc822"
    extension?: string;        // e.g. ".eml"
  };

  // 2. Semantic Info (Populated during Step 1)
  emailInfo?: {
    subject: string;      // e.g. "Invoice #1024"
    sender: string;       // e.g. "noreply@amazon.com"
    recipients?: string[];
    sentDate?: string;     // ISO Date
  };
  
  // 3. AI Results (Populated at the end)
  prediction?: {
    category?: string;     // UUID
    context: string;      // UUID
    summary?: string;
    documentDate?: string; // ISO Date
    confidence?: number;
    customAttributes?: Record<string, any>;
    manuallyAssigned?: boolean;
  };
  
  // Helper for UI (optional override)
  displayName?: string;
}

export interface ProgressMessage {
  sha256: string;
  info: string; // e.g., "Extracting Text (OCR)..."
  step?: number;
  totalSteps?: number;
}

export interface PipelineStatusMessage {
  sha256: string;
  uuid?: string | null; // Null during staging
  status: 'READY' | 'DUPLICATE' | 'ERROR' | 'PROCESSING';
  fileState?: InboxFile; // The complete updated state passed from backend
  warnings?: string[];
  originalFilename?: string; // Legacy fallback
}

export interface BroadcastMessage {
  process: 'INBOX' | 'PIPELINE';
  // Added 'COMPLETE' to match usage in useServerEvents.ts
  type: 'FILE_ADDED' | 'PROGRESS' | 'STATUS' | 'ERROR' | 'COMPLETE';
  messageType?: string;
  message: InboxFile | ProgressMessage | PipelineStatusMessage | any;
}