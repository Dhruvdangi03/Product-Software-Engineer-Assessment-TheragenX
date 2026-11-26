import type { FieldResult } from '../api/types';

/**
 * How much attention a field needs, from most to least. A conflict needs a decision, a
 * low-confidence value is the likeliest to be wrong, an unknown is a gap to fill.
 */
export type Risk = 'conflict' | 'low' | 'medium' | 'unknown' | 'high';

export const RISKS_BY_PRIORITY: Risk[] = ['conflict', 'low', 'medium', 'unknown', 'high'];

export const RISK_LABELS: Record<Risk, string> = {
  conflict: 'Conflict',
  low: 'Low confidence',
  medium: 'Medium confidence',
  unknown: 'Unknown',
  high: 'High confidence',
};

export function riskOf(result: FieldResult | undefined): Risk {
  if (!result || result.status === 'unknown') {
    return 'unknown';
  }
  if (result.status === 'conflict') {
    return 'conflict';
  }
  // An extracted value without confidence is scored as high by the checker; show it the same way.
  return result.confidence ?? 'high';
}

/** Risks a reviewer should look at first. Unknown fields are gaps, not risks, so they are left out. */
export function needsAttention(risk: Risk): boolean {
  return risk === 'conflict' || risk === 'low' || risk === 'medium';
}
