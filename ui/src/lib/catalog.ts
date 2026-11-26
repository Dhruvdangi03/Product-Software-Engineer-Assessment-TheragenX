import type { Catalog, FieldDefinition, FieldValue, Section } from '../api/types';

export interface SectionGroup {
  section: Section;
  fields: FieldDefinition[];
}

/** The catalog's fields grouped by section, both in catalog order. Empty sections are left out. */
export function groupBySection(catalog: Catalog): SectionGroup[] {
  return catalog.sections
    .map((section) => ({ section, fields: catalog.fields.filter((field) => field.section === section.id) }))
    .filter((group) => group.fields.length > 0);
}

/** Fields in the order the panel shows them: by section, then by catalog order. */
export function fieldsInDisplayOrder(catalog: Catalog): FieldDefinition[] {
  return groupBySection(catalog).flatMap((group) => group.fields);
}

export function optionLabel(field: FieldDefinition, code: string): string {
  return field.options.find((option) => option.code === code)?.label ?? code;
}

/** A value as the reviewer reads it: option labels instead of codes, lists joined. */
export function formatValue(field: FieldDefinition, value: FieldValue | undefined): string {
  if (value === undefined) {
    return '';
  }
  switch (field.type) {
    case 'coded':
      return optionLabel(field, String(value));
    case 'coded_multi':
      return asList(value).map((code) => optionLabel(field, code)).join(', ');
    case 'string_list':
      return asList(value).length === 0 ? 'None' : asList(value).join(', ');
    case 'date':
      return formatDate(String(value));
    default:
      return String(value);
  }
}

export function asList(value: FieldValue | undefined): string[] {
  if (value === undefined) {
    return [];
  }
  return Array.isArray(value) ? value : [String(value)];
}

const DATE_FORMAT = new Intl.DateTimeFormat('en-GB', { day: 'numeric', month: 'short', year: 'numeric', timeZone: 'UTC' });

function formatDate(iso: string): string {
  const date = new Date(`${iso}T00:00:00Z`);
  return Number.isNaN(date.getTime()) ? iso : `${DATE_FORMAT.format(date)}`;
}
