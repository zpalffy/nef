# nef

A personal journaling tool. You write markdown files, nef helps you read them back.

That's basically it.

## The idea

Your journal is just a folder of text files. Name them with a date and a title, write whatever you want inside, and nef turns that folder into something you can browse in a browser — locally, or on your phone over your home network.

No app lock-in. No subscription. No proprietary format. Your files are just files. They'll outlive any app.

## Naming your files

The filename is all the metadata you need:

```
2024-06-23 The morning I saw a deer.md
2024-06-15 Thinking about leaving the city.md
My first entry.md
```

- Start with a date (`YYYY-MM-DD`) and it'll be used for ordering
- No date? nef falls back to the file's modification time
- Tags and author are supported too if you ever want them: `2024-06-23 My Post [tag1 tag2] (Author).md`

Most of the time you probably just want date + title, and the rest takes care of itself.

## Getting started

You'll need [Node.js](https://nodejs.org) installed. Then:

```sh
git clone https://github.com/zpalffy/nef
cd nef
npm install
```

Point it at a folder of markdown files:

```sh
node nef.js -ep -d ~/journal
```

That writes `entries.json` into your journal folder. Then serve it:

```sh
python3 -m http.server 8080 --bind 0.0.0.0 --directory ~/journal
```

Open `http://localhost:8080` in your browser and you're reading your journal.

## Options

```
-d, --directory <dir>     directory to scan (default: current)
-e, --entries             write entries.json (otherwise prints to stdout)
-p, --pretty-print        pretty print the JSON
-r, --recursive           include subdirectories
-a, --author <name>       default author for all entries
-t, --tag-separator <c>   character used to separate tags (default: space)
--extensions <exts>       comma-separated file extensions (default: md,markdown,txt)
```

## Reading on your phone

Since the Python server binds to all interfaces (`0.0.0.0`), any device on your home network can reach it. Find your machine's local IP address and open `http://192.168.x.x:8080` on your phone.

## The bigger picture

Someday this might grow a `nef serve` command that watches for new files and rebuilds automatically, and maybe even a way to send your journal off to a print-on-demand service and get a physical book back. But for now: write stuff, run the command, read it back.
