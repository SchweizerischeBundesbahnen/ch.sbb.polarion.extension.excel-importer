import { describe, expect, it } from 'vitest';
// @ts-expect-error - a plain ESM build script without type declarations
import { sectionsOf } from '../scripts/docs-sections.mjs';
import config from '../src/docs/docs.config.json';
import searchIndex from '../src/docs/search-index.json';

// The documentation search index (scripts/build-docs-index.mjs). The other documentation tests replace the index
// with fixed records, so they would pass whatever the builder produces: this file tests the builder itself.

interface Section {
  anchor: string;
  title: string;
  text: string;
}

// The shape markdown2html renders: a flat sequence of headings carrying generated ids, paragraphs, tables and
// code blocks whose tokens are wrapped in syntax-highlighting spans.
const ARTICLE = `
<h1 id="user-guide">User Guide</h1>
<p>Intro before the first section.</p>
<h2 id="mappings">Mappings</h2>
<p>A project can have any number of mappings.</p>
<h3 id="general-settings">General settings</h3>
<table><tbody><tr><td>Filter by query</td><td>A Lucene query.</td></tr></tbody></table>
<h4 id="enum-dependencies">Enum dependencies</h4>
<p>Polarion lets a custom enum field depend on another field.</p>
<h5 id="detail">Detail</h5>
<p>Text under an h5 belongs to no section.</p>
<h2 id="rest-api-2">REST API</h2>
<pre><code class="language-bash"><span style="color:#8250df">curl</span> -F <span>"mappingName=Requirements"</span></code></pre>
`;

describe('docs search index builder', () => {
  const sections: Section[] = sectionsOf(ARTICLE);

  it('makes one section per h2, h3 and h4 with the id of the heading as anchor', () => {
    expect(sections.map((s) => [s.anchor, s.title])).toEqual([
      ['mappings', 'Mappings'],
      ['general-settings', 'General settings'],
      ['enum-dependencies', 'Enum dependencies'],
      ['rest-api-2', 'REST API'],
    ]);
  });

  it('ends a section at the next heading of any level', () => {
    expect(sections[0].text).toBe('A project can have any number of mappings.');
    expect(sections[1].text).toContain('Filter by query');
    expect(sections[1].text).not.toContain('custom enum field');
    expect(sections[2].text).toBe('Polarion lets a custom enum field depend on another field.');
  });

  it('leaves out text before the first section and under h5/h6', () => {
    const all = sections.map((s) => s.text).join(' ');
    expect(all).not.toContain('Intro before the first section');
    expect(all).not.toContain('belongs to no section');
  });

  it('keeps the text of code blocks without their highlighting markup', () => {
    expect(sections[3].text).toBe('curl -F "mappingName=Requirements"');
  });

  it('finds no section in markup of another structure', () => {
    // build-docs-index.mjs --require fails the build on this, rather than shipping an empty search.
    expect(sectionsOf('<div><h2 id="a">A</h2><p>Text.</p></div>')).toEqual([]);
  });
});

// The index the build generated from the real articles. The Maven build renders them before the JS suite runs,
// so these assertions are real there; a plain `npm test` without them gets the empty dev-mode index and skips.
interface SearchRecord extends Section {
  doc: string;
  docTitle: string;
}

const RECORDS = searchIndex as SearchRecord[];

describe.skipIf(RECORDS.length === 0)('generated documentation search index', () => {
  it('covers every article of the manifest and nothing else', () => {
    expect(new Set(RECORDS.map((r) => r.doc))).toEqual(new Set(config.items.map((i) => i.id)));
  });

  it('carries a title and a slug-form anchor per record', () => {
    for (const record of RECORDS) {
      expect(record.title.length).toBeGreaterThan(0);
      expect(record.anchor).toMatch(/^[a-z0-9]+(?:-[a-z0-9]+)*$/);
    }
  });

  it('indexes the h4 subsections of the user guide', () => {
    const userGuide = RECORDS.filter((r) => r.doc === 'user-guide');
    expect(userGuide.find((r) => r.anchor === 'enum-dependencies')?.text).toContain('controlling field');
    expect(userGuide.find((r) => r.anchor === 'updating-only-some-work-items')?.text).toContain('Lucene');
  });

  it('carries the text of code blocks', () => {
    const restApi = RECORDS.filter((r) => r.doc === 'rest-api').map((r) => r.text);
    expect(restApi.some((text) => text.includes('mappingName=Requirements'))).toBe(true);
  });
});
