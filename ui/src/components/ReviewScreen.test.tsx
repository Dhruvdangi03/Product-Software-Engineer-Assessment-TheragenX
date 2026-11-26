import { fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { api } from '../api/client';
import type { DocumentDetail } from '../api/types';
import { testCatalog, testDetail } from '../test/fixtures';
import { ReviewScreen } from './ReviewScreen';

const expanded = () => screen.getAllByRole('button', { expanded: true });
const rowLabel = (button: HTMLElement) => button.querySelector('.field-row__label')?.textContent;

describe('ReviewScreen', () => {
  beforeEach(() => vi.restoreAllMocks());

  it('moves through fields with the keyboard', async () => {
    render(<ReviewScreen catalog={testCatalog} detail={testDetail} onReviewSaved={vi.fn()} />);

    await userEvent.keyboard('j');
    expect(rowLabel(expanded()[0]!)).toBe('Code name');
    await userEvent.keyboard('j');
    expect(rowLabel(expanded()[0]!)).toBe('Colour');
    await userEvent.keyboard('k');
    expect(rowLabel(expanded()[0]!)).toBe('Code name');
  });

  it('highlights the selected field quote and selects a field from its quote', async () => {
    render(<ReviewScreen catalog={testCatalog} detail={testDetail} onReviewSaved={vi.fn()} />);

    fireEvent.click(screen.getByText('Colour red', { selector: 'mark' }));

    expect(rowLabel(expanded()[0]!)).toBe('Colour');
    expect(screen.getByText('Colour red', { selector: 'mark' })).toHaveClass('is-selected');
  });

  it('accepts the selected field with A and moves on', async () => {
    const saved = { cases: { '0': { code_name: { decision: 'accepted' as const, reviewed_at: 'now' } } } };
    const reviewField = vi.spyOn(api, 'reviewField').mockResolvedValue(saved);
    const onReviewSaved = vi.fn();
    render(<ReviewScreen catalog={testCatalog} detail={testDetail} onReviewSaved={onReviewSaved} />);

    await userEvent.keyboard('ja');

    expect(reviewField).toHaveBeenCalledWith('T1', 0, 'code_name', 'accept', undefined);
    expect(onReviewSaved).toHaveBeenCalledWith(saved);
    expect(rowLabel(expanded()[0]!)).toBe('Colour');
  });

  it('explains a document without a case', () => {
    const noCase: DocumentDetail = {
      ...testDetail,
      extraction: { ...testDetail.extraction!, cases: [], flags: ['no_adverse_event'] },
    };

    render(<ReviewScreen catalog={testCatalog} detail={noCase} onReviewSaved={vi.fn()} />);

    expect(screen.getByText('This document does not describe a reportable case')).toBeInTheDocument();
    expect(screen.getByText(/nobody had a reaction/)).toBeInTheDocument();
  });
});
