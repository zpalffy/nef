#!/usr/bin/env node

'use strict';

const fs = require('fs');
const path = require('path');

function parseDate(str) {
  // datetime with optional timezone: 2024-01-15T120000, 2024-01-15T120000Z, 2024-01-15T120000+0530
  let m = str.match(/^(\d{4}-\d{2}-\d{2})T(\d{2})(\d{2})(\d{2})(Z|[+-]\d{4})?$/);
  if (m) {
    const [, date, hh, mm, ss, tz] = m;
    const tzPart = !tz ? '' : tz === 'Z' ? 'Z' : `${tz.slice(0, 3)}:${tz.slice(3)}`;
    const d = new Date(`${date}T${hh}:${mm}:${ss}${tzPart}`);
    if (!isNaN(d.getTime())) return d.getTime();
  }
  // date only: 2024-01-15
  m = str.match(/^(\d{4})-(\d{2})-(\d{2})$/);
  if (m) {
    const d = new Date(Number(m[1]), Number(m[2]) - 1, Number(m[3]));
    if (!isNaN(d.getTime())) return d.getTime();
  }
  return null;
}

function slugify(title) {
  return title
    .toLowerCase()
    .replace(/ /g, '-')
    .replace(/[!"#$%&'()*+,./:;<=>?@\[\]\\^`{|}~]/g, '');
}

function parseFilename(basename, mtime, size, tagSeparator = ' ') {
  let title = basename.trim();
  let ts = null;

  const spaceIdx = title.indexOf(' ');
  const firstToken = spaceIdx === -1 ? title : title.substring(0, spaceIdx);

  if (/^\d{4}-\d{2}-\d{2}/.test(firstToken)) {
    const parsed = parseDate(firstToken);
    if (parsed !== null) {
      ts = parsed;
      title = spaceIdx === -1 ? '' : title.substring(spaceIdx + 1).trim();
    }
  }

  if (ts === null) ts = mtime;

  // extract tags from [tag1 tag2]
  let tags = null;
  const tagMatch = title.match(/\[([^\]]*)\]/);
  if (tagMatch) {
    const tagStr = tagMatch[1].trim();
    if (tagStr) tags = tagStr.split(tagSeparator).map(t => t.trim()).filter(Boolean);
    title = title.replace(/\s*\[[^\]]*\]\s*/, ' ').trim();
  }

  // extract author from (Author Name)
  let author = null;
  const authorMatch = title.match(/\(([^)]*)\)/);
  if (authorMatch) {
    const authorStr = authorMatch[1].trim();
    if (authorStr) author = authorStr;
    title = title.replace(/\s*\([^)]*\)\s*/, ' ').trim();
  }

  const slug = title ? slugify(title) : new Date(ts).toISOString().substring(0, 10);

  const entry = { slug, ts: Math.round(ts), modified: Math.round(mtime), size };
  if (title) entry.title = title;
  if (tags) entry.tags = tags;
  if (author) entry.author = author;

  return entry;
}

function collectEntries(dir, extensions, recursive, tagSeparator, defaultAuthor) {
  const entries = [];

  function scan(current) {
    for (const name of fs.readdirSync(current).sort()) {
      if (name.startsWith('.')) continue;

      const full = path.join(current, name);
      const stat = fs.statSync(full);

      if (stat.isDirectory()) {
        if (recursive) scan(full);
        continue;
      }

      const ext = path.extname(name).slice(1).toLowerCase();
      if (!extensions.includes(ext)) continue;
      if (stat.size === 0) continue;

      const basename = path.basename(name, path.extname(name));
      const entry = parseFilename(basename, stat.mtimeMs, stat.size, tagSeparator);
      entry.path = '/' + path.relative(dir, full);
      if (!entry.author && defaultAuthor) entry.author = defaultAuthor;

      entries.push(entry);
    }
  }

  scan(dir);
  return entries;
}

if (require.main === module) {
  const { Command } = require('commander');
  const program = new Command();

  program
    .name('nef')
    .description('Generate JSON metadata for writing entries in a directory.')
    .option('-d, --directory <dir>', 'directory to scan', '.')
    .option('-e, --entries', 'write entries.json file (otherwise stdout)')
    .option('--extensions <exts>', 'comma-separated file extensions', 'md,markdown,txt')
    .option('-a, --author <author>', 'default author')
    .option('-p, --pretty-print', 'pretty print JSON')
    .option('-r, --recursive', 'search subdirectories')
    .option('-t, --tag-separator <char>', 'tag separator character', ' ')
    .parse();

  const opts = program.opts();
  const dir = path.resolve(opts.directory);
  const extensions = opts.extensions.split(',').map(e => e.trim().toLowerCase());

  const entries = collectEntries(dir, extensions, opts.recursive, opts.tagSeparator, opts.author);
  entries.sort((a, b) => b.ts - a.ts);

  const json = opts.prettyPrint ? JSON.stringify(entries, null, 2) : JSON.stringify(entries);

  if (opts.entries) {
    fs.writeFileSync(path.join(dir, 'entries.json'), json, 'utf8');
  } else {
    process.stdout.write(json + '\n');
  }
}

module.exports = { parseDate, slugify, parseFilename };
