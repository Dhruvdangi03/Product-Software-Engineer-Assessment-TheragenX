import { readdirSync, readFileSync, statSync } from 'node:fs';
import { join } from 'node:path';
import { describe, expect, it } from 'vitest';

/** The UI must render any catalog: no catalog field id may appear in its source code. */
describe('UI source', () => {
  it('names no field id of the catalog', () => {
    const catalog = JSON.parse(readFileSync(join(__dirname, '../../../catalog/field_catalog.json'), 'utf-8')) as { fields: { id: string }[] };
    const sources = listSources(join(__dirname, '..'));

    for (const file of sources) {
      const content = readFileSync(file, 'utf-8');
      for (const { id } of catalog.fields) {
        expect(content.includes(id), `${file} mentions ${id}`).toBe(false);
      }
    }
  });
});

function listSources(dir: string): string[] {
  return readdirSync(dir).flatMap((name) => {
    const path = join(dir, name);
    if (statSync(path).isDirectory()) {
      return name === 'test' ? [] : listSources(path);
    }
    return /\.(ts|tsx)$/.test(name) && !/\.test\.tsx?$/.test(name) ? [path] : [];
  });
}
