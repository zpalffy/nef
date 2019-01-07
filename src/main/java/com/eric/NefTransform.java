package com.eric;

import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;

import com.beust.jcommander.Parameter;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class NefTransform extends Command {

	@Parameter(names = { "--dir", "-d" }, description = "The directory to work from.  Defaults to the current directory.", order = 0)
	private String dir = getWorkingDirectory();

	@Parameter(names = { "--extensions", "-e" }, description = "The file extensions to consider when creating entries.  Note: do not include the '.' character", order = 1)
	private List<String> extensions = Lists.newArrayList("md", "markdown", "txt");

	@Parameter(names = { "--author", "-a" }, description = "The default author to use if one is not specified for an entry.", order = 2)
	private String defaultAuthor;

	@Parameter(names = { "--empty-files", "-ef" }, description = "Whether to write metadata files if they contain no values.  If false, files will be deleted if they already exist and there are no values.", order = 3)
	private boolean emptyFiles;

	@Parameter(names = { "--pretty-print", "-p" }, description = "Pretty print all json files.", order = 4)
	private boolean prettyPrint;

	@Parameter(names = { "--tag-separator", "-t" }, description = "The character to use to separate tags.  Defaults to the space character.", order = 5)
	private char tagSeparator = ' ';

	@Parameter(names = { "--time-zone", "-tz" }, description = "The time zone to use when parsing date/times in file names", order = 6)
	private String timezone = TimeZone.getDefault().getID();

	private Gson gson;

	private void writeJson(File file, Object obj, boolean empty) {
		if (empty && !emptyFiles) { // no values and we are not writing empty
									// files, delete if exists:
			if (file.exists()) {
				verbose("Deleting file %s since it already exists, and we are not writing files with no values", file);
				file.delete();
			}
		} else {
			try {
				verbose("Writing file %s", file);
				FileWriter fw = new FileWriter(file);
				gson.toJson(obj, fw);
				fw.close();
			} catch (IOException ex) {
				err("An error occured while writing JSON to the file: " + file, ex);
			}
		}
	}

	@Override
	protected String getProgramName() {
		return "nef";
	}

	@Override
	protected void beforeValidate() {
		GsonBuilder builder = new GsonBuilder();
		if (prettyPrint) {
			builder.setPrettyPrinting();
		}
		gson = builder.create();
	}

	@Override
	protected void validate(Collection<String> messages) {
		if (extensions.isEmpty()) {
			messages.add("At least one extension is required.");
		}
	}

	private void processDir(File dir, int baseDirLength) throws Exception {
		Map<String, Long> authors = new TreeMap<>();
		Map<String, Long> tags = new TreeMap<>();

		List<Entry> entries = Lists.newArrayList();
		for (File file : FileUtils.listFiles(dir, extensions.toArray(new String[0]), true)) {
			if (!file.isHidden()) {
				Entry entry = Entry.parse(file, tagSeparator, TimeZone.getTimeZone(timezone), baseDirLength);
				if (entry.getAuthor() == null && defaultAuthor != null) {
					entry.setAuthor(defaultAuthor);
				}

				entries.add(entry);

				if (entry.getAuthor() != null) {
					authors.put(entry.getAuthor(),
							authors.containsKey(entry.getAuthor()) ? authors.get(entry.getAuthor()) + 1 : 1);
				}

				if (entry.getTags() != null) {
					for (String tag : entry.getTags()) {
						tags.put(tag, tags.containsKey(tag) ? tags.get(tag) + 1 : 1);
					}
				}
			}
		}
		entries.sort(null); // sort by natural order, aka timestamp

		writeJson(new File(dir, "entries.json"), entries, entries.isEmpty());
		writeJson(new File(dir, "authors.json"), authors, authors.isEmpty());
		writeJson(new File(dir, "tags.json"), tags, tags.isEmpty());

		TreeSet<String> dirs = new TreeSet<>();
		for (File subdir : dir.listFiles((FileFilter) FileFilterUtils.directoryFileFilter())) {
			processDir(subdir, baseDirLength);
			dirs.add(subdir.getName());
		}
		writeJson(new File(dir, "directories.json"), dirs, dirs.isEmpty());
	}

	@Override
	protected void run() throws Exception {
		long start = System.currentTimeMillis();
		File f = new File(expandHomeDir(dir));
		processDir(f, f.getAbsolutePath().length());
		verbose("Processing took %s ms.", System.currentTimeMillis() - start);
	}

	public static void main(String[] args) {
		Command.main(new NefTransform(), args);
	}
}
