import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import type { FieldDefinition } from '../api/types';
import { testCatalog } from '../test/fixtures';
import { FieldEditor } from './FieldEditor';

const field = (id: string): FieldDefinition => testCatalog.fields.find((f) => f.id === id)!;

describe('FieldEditor picks its control from the catalog type', () => {
  it('shows the options of a coded field', () => {
    render(<FieldEditor field={field('colour')} onSave={vi.fn()} onCancel={vi.fn()} />);

    const select = screen.getByRole('combobox');
    expect([...select.querySelectorAll('option')].map((o) => o.textContent)).toEqual(['Choose…', 'Red', 'Blue']);
  });

  it('uses a date control for a date field', () => {
    render(<FieldEditor field={field('visit_date')} onSave={vi.fn()} onCancel={vi.fn()} />);

    expect(screen.getByLabelText('New value for Visit date')).toHaveAttribute('type', 'date');
  });

  it('uses a multi-select for a coded_multi field and saves the chosen codes', async () => {
    const onSave = vi.fn();
    render(<FieldEditor field={field('tags')} onSave={onSave} onCancel={vi.fn()} />);

    await userEvent.click(screen.getByLabelText('Tag A'));
    await userEvent.click(screen.getByLabelText('Tag B'));
    await userEvent.click(screen.getByRole('button', { name: 'Save' }));

    expect(onSave).toHaveBeenCalledWith(['a', 'b']);
  });

  it('saves a list as one trimmed item per line', async () => {
    const onSave = vi.fn();
    render(<FieldEditor field={field('notes')} onSave={onSave} onCancel={vi.fn()} />);

    await userEvent.type(screen.getByRole('textbox'), ' rash {enter}{enter}itch');
    await userEvent.click(screen.getByRole('button', { name: 'Save' }));

    expect(onSave).toHaveBeenCalledWith(['rash', 'itch']);
  });

  it('shows the error the backend returned', () => {
    render(<FieldEditor field={field('colour')} error="not one of the options" onSave={vi.fn()} onCancel={vi.fn()} />);

    expect(screen.getByRole('alert')).toHaveTextContent('not one of the options');
  });
});
