'use strict';

const { describe, test } = require('node:test');
const assert = require('node:assert/strict');
const { parseDate, slugify, parseFilename } = require('./nef');

const MTIME = new Date(2023, 5, 15).getTime(); // June 15 2023
const SIZE = 1024;

describe('parseDate', () => {
  test('date only', () => {
    assert.equal(parseDate('2024-01-15'), new Date(2024, 0, 15).getTime());
  });

  test('datetime without timezone', () => {
    assert.equal(parseDate('2024-01-15T120000'), new Date('2024-01-15T12:00:00').getTime());
  });

  test('datetime with Z', () => {
    assert.equal(parseDate('2024-01-15T120000Z'), new Date('2024-01-15T12:00:00Z').getTime());
  });

  test('datetime with offset', () => {
    assert.equal(parseDate('2024-01-15T120000+0530'), new Date('2024-01-15T12:00:00+05:30').getTime());
  });

  test('returns null for non-dates', () => {
    assert.equal(parseDate('My Post'), null);
    assert.equal(parseDate('2024'), null);
    assert.equal(parseDate('2024-01'), null);
  });
});

describe('slugify', () => {
  test('lowercases and hyphenates spaces', () => {
    assert.equal(slugify('My Post Title'), 'my-post-title');
  });

  test('removes punctuation', () => {
    assert.equal(slugify('Hello, World!'), 'hello-world');
    assert.equal(slugify('My Post: A Story'), 'my-post-a-story');
  });

  test('preserves hyphens and underscores', () => {
    assert.equal(slugify('My_Post-Title'), 'my_post-title');
  });
});

describe('parseFilename', () => {
  test('date and title', () => {
    const e = parseFilename('2024-01-15 My Post', MTIME, SIZE);
    assert.equal(e.title, 'My Post');
    assert.equal(e.slug, 'my-post');
    assert.equal(e.ts, new Date(2024, 0, 15).getTime());
    assert.equal(e.modified, MTIME);
    assert.equal(e.size, SIZE);
  });

  test('no date falls back to mtime', () => {
    const e = parseFilename('My Post', MTIME, SIZE);
    assert.equal(e.title, 'My Post');
    assert.equal(e.ts, MTIME);
  });

  test('date only filename uses date as slug', () => {
    const e = parseFilename('2024-01-15', MTIME, SIZE);
    assert.equal(e.title, undefined);
    assert.equal(e.slug, '2024-01-15');
  });

  test('parses tags', () => {
    const e = parseFilename('2024-01-15 My Post [tag1 tag2]', MTIME, SIZE);
    assert.deepEqual(e.tags, ['tag1', 'tag2']);
    assert.equal(e.title, 'My Post');
  });

  test('parses author', () => {
    const e = parseFilename('2024-01-15 My Post (John Doe)', MTIME, SIZE);
    assert.equal(e.author, 'John Doe');
    assert.equal(e.title, 'My Post');
  });

  test('parses tags and author together', () => {
    const e = parseFilename('2024-01-15 My Post [tag1 tag2] (John Doe)', MTIME, SIZE);
    assert.deepEqual(e.tags, ['tag1', 'tag2']);
    assert.equal(e.author, 'John Doe');
    assert.equal(e.title, 'My Post');
  });

  test('omits undefined fields', () => {
    const e = parseFilename('My Post', MTIME, SIZE);
    assert.equal(e.tags, undefined);
    assert.equal(e.author, undefined);
  });

  test('custom tag separator', () => {
    const e = parseFilename('2024-01-15 My Post [tag1,tag2]', MTIME, SIZE, ',');
    assert.deepEqual(e.tags, ['tag1', 'tag2']);
  });

  test('datetime in filename', () => {
    const e = parseFilename('2024-01-15T120000 My Post', MTIME, SIZE);
    assert.equal(e.ts, new Date('2024-01-15T12:00:00').getTime());
    assert.equal(e.title, 'My Post');
  });

  test('title with punctuation is slugified', () => {
    const e = parseFilename('2024-01-15 It\'s Alive!', MTIME, SIZE);
    assert.equal(e.slug, 'its-alive');
  });
});
