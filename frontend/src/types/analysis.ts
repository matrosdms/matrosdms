/**
 * Strict Type definition for AI Analysis payloads coming from SSE/Backend.
 * Internal Frontend State representation.
 */
export interface InboxAnalysis {
    // Backend Enum Mapping: PENDING | PROCESSING | DUPLICATE | READY | ERROR
    status: 'PENDING' | 'PROCESSING' | 'COMPLETE' | 'DUPLICATE' | 'ERROR' | 'READY';
    
    // Prediction Fields (Mapped from backend 'prediction' object)
    predictedContext?: string; // UUID
    predictedCategory?: string; // UUID
    predictedStore?: string;   // UUID (Physical Location) <--- NEW
    predictedDate?: string; // ISO Date String
    predictedDescription?: string;
    
    // Validation flags
    isDuplicate?: boolean;
    contentHash?: string;
    originalFilename?: string;
    
    // Dynamic Attributes (UUID -> Value)
    customAttributes?: Record<string, any>;
    
    // Metadata
    timestamp?: string;
    sha256?: string;
    
    // Pipeline Feedback
    warnings?: string[];
}