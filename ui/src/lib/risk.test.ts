import { describe, expect, it } from 'vitest';
import { needsAttention, riskOf } from './risk';

describe('riskOf', () => {
  it('ranks conflicts, confidence and unknowns', () => {
    expect(riskOf({ status: 'conflict' })).toBe('conflict');
    expect(riskOf({ status: 'unknown' })).toBe('unknown');
    expect(riskOf(undefined)).toBe('unknown');
    expect(riskOf({ status: 'extracted', value: 'x', confidence: 'low' })).toBe('low');
  });

  it('treats a value without confidence as high, as the checker does', () => {
    expect(riskOf({ status: 'extracted', value: 'x' })).toBe('high');
  });

  it('flags conflicts and uncertain values for attention, not gaps', () => {
    expect(needsAttention('conflict')).toBe(true);
    expect(needsAttention('medium')).toBe(true);
    expect(needsAttention('unknown')).toBe(false);
    expect(needsAttention('high')).toBe(false);
  });
});
