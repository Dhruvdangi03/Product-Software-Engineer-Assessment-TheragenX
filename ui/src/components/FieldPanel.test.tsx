import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import type { Catalog } from '../api/types';
import { testCatalog, testDetail } from '../test/fixtures';
import { FieldPanel, type RiskFilter } from './FieldPanel';

function renderPanel(catalog: Catalog = testCatalog, filter: RiskFilter = 'all', onFilterChange = vi.fn()) {
  return render(
    <FieldPanel
      catalog={catalog}
      extractedCase={testDetail.extraction!.cases[0]!}
      reviews={{}}
      filter={filter}
      onFilterChange={onFilterChange}
      errorByField={{}}
      onSelectField={vi.fn()}
      onReview={vi.fn()}
      onStartEdit={vi.fn()}
      onCancelEdit={vi.fn()}
    />,
  );
}

describe('FieldPanel', () => {
  it('renders sections and fields in catalog order with catalog labels', () => {
    renderPanel();

    const sections = screen.getAllByRole('heading', { level: 3 }).map((h) => h.textContent);
    expect(sections).toEqual(['Person', 'Visit']);
    const visit = screen.getByRole('region', { name: 'Visit' });
    const labels = within(visit).getAllByRole('button').map((b) => b.querySelector('.field-row__label')?.textContent);
    expect(labels).toEqual(['Colour', 'Visit date', 'Tags', 'Notes']);
  });

  it('shows option labels, conflicts and unknowns', () => {
    renderPanel();

    expect(screen.getByText('Red')).toBeInTheDocument();
    expect(screen.getByText('Document contradicts itself')).toBeInTheDocument();
    expect(screen.getByText('Not stated')).toBeInTheDocument();
  });

  it('renders a field added to the catalog without any code change', () => {
    const extended: Catalog = {
      ...testCatalog,
      fields: [...testCatalog.fields, { id: 'brand_new', label: 'Brand new field', section: 'person', type: 'integer', options: [], validation: {} }],
    };

    renderPanel(extended);

    expect(screen.getByText('Brand new field')).toBeInTheDocument();
  });

  it('counts fields by risk and filters by it', async () => {
    const onFilterChange = vi.fn();
    renderPanel(testCatalog, 'all', onFilterChange);

    await userEvent.click(screen.getByRole('button', { name: /Conflict 1/ }));
    expect(onFilterChange).toHaveBeenCalledWith('conflict');
  });

  it('hides fields of other risks when filtered', () => {
    renderPanel(testCatalog, 'conflict');

    expect(screen.getByText('Visit date')).toBeInTheDocument();
    expect(screen.queryByText('Code name')).not.toBeInTheDocument();
  });
});
