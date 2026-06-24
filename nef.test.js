'use strict';

const { parseDate, slugify, parseFilename } = require('./nef');

const MTIME = new Date(2023, 5, 15).getTime(); // June 15 2023
const SIZE = 1024;

describe('parseDate', () => {
  test('date only', () => {
    expect(parseDate('2024-01-15')).toBe(new Date(2024, 0, 15).getTime());
  });

  test('datetime without timezone', () => {
    expect(parseDate('2024-01-15T120000')).toBe(new Date('2024-01-15T12:00:00').getTime());
  });

  test('datetime with Z', () => {
    expect(parseDate('2024-01-15T120000Z')).toBe(new Date('2024-01-15T12:00:00Z').getTime());
  });

  test('datetime with offset', () => {
    expect(parseDate('2024-01-15T120000+0530')).toBe(new Date('2024-01-15T12:00:00+05:30').getTime());
  });

  test('returns null for non-dates', () => {
    expect(parseDate('My Post')).toBeNull();
    expect(parseDate('2024')).toBeNull();
    expect(parseDate('2024-01')).toBeNull();
  });
});

describe('slugify', () => {
  test('lowercases and hyphenates spaces', () => {
    expect(slugify('My Post Title')).toBe('my-post-title');
  });

  test('removes punctuation', () => {
    expect(slugify('Hello, World!')).toBe('hello-world');
    expect(slugify('My Post: A Story')).toBe('my-post-a-story');
  });

  test('preserves hyphens and underscores', () => {
    expect(slugify('My_Post-Title')).toBe('my_post-title');
  });
});

describe('parseFilename', () => {
  test('date and title', () => {
    const e = parseFilename('2024-01-15 My Post', MTIME, SIZE);
    expect(e.title).toBe('My Post');
    expect(e.slug).toBe('my-post');
    expect(e.ts).toBe(new Date(2024, 0, 15).getTime());
    expect(e.modified).toBe(MTIME);
    expect(e.size).toBe(SIZE);
  });

  test('no date falls back to mtime', () => {
    const e = parseFilename('My Post', MTIME, SIZE);
    expect(e.title).toBe('My Post');
    expect(e.ts).toBe(MTIME);
  });

  test('date only filename uses date as slug', () => {
    const e = parseFilename('2024-01-15', MTIME, SIZE);
    expect(e.title).toBeUndefined();
    expect(e.slug).toBe('2024-01-15');
  });

  test('parses tags', () => {
    const e = parseFilename('2024-01-15 My Post [tag1 tag2]', MTIME, SIZE);
    expect(e.tags).toEqual(['tag1', 'tag2']);
    expect(e.title).toBe('My Post');
  });

  test('parses author', () => {
    const e = parseFilename('2024-01-15 My Post (John Doe)', MTIME, SIZE);
    expect(e.author).toBe('John Doe');
    expect(e.title).toBe('My Post');
  });

  test('parses tags and author together', () => {
    const e = parseFilename('2024-01-15 My Post [tag1 tag2] (John Doe)', MTIME, SIZE);
    expect(e.tags).toEqual(['tag1', 'tag2']);
    expect(e.author).toBe('John Doe');
    expect(e.title).toBe('My Post');
  });

  test('omits undefined fields', () => {
    const e = parseFilename('My Post', MTIME, SIZE);
    expect(e.tags).toBeUndefined();
    expect(e.author).toBeUndefined();
  });

  test('custom tag separator', () => {
    const e = parseFilename('2024-01-15 My Post [tag1,tag2]', MTIME, SIZE, ',');
    expect(e.tags).toEqual(['tag1', 'tag2']);
  });

  test('datetime in filename', () => {
    const e = parseFilename('2024-01-15T120000 My Post', MTIME, SIZE);
    expect(e.ts).toBe(new Date('2024-01-15T12:00:00').getTime());
    expect(e.title).toBe('My Post');
  });

  test('title with punctuation is slugified', () => {
    const e = parseFilename('2024-01-15 It\'s Alive!', MTIME, SIZE);
    expect(e.slug).toBe('its-alive');
  });
});
