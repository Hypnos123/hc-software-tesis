export type ClinicalHistoryFlowFeedbackType = 'prefill-success' | 'prefill-failure';

export type ClinicalHistoryFlowFeedback =
  | { id: string; type: 'prefill-success'; createdAt: number }
  | { id: string; type: 'prefill-failure'; createdAt: number };
