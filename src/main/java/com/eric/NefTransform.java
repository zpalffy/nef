package com.eric;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.Callable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.commons.io.FileUtils;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "nef", mixinStandardHelpOptions = true, version = { "nef 1.0",
		"https://github.com/zpalffy/nef" }, description = "Utility to generate json metadata file for writing entries in directories.")
public class NefTransform implements Callable<Integer> {

	@Parameters(description = "The directory to work from.  Defaults to the current directory (${DEFAULT-VALUE})")
	private Path[] paths = new Path[] { Paths.get(".") };

	@Option(names = { "--extensions",
			"-e" }, description = "The file extensions to consider when creating entries.  Note: do not include the '.' character (default: ${DEFAULT-VALUE})")
	private String[] extensions = new String[] { "md", "markdown", "txt" };

	@Option(names = { "--file",
			"-f" }, description = "The json file to output relative to each directory (default: ${DEFAULT-VALUE})")
	private String fileName = "entries.json";

	@Option(names = { "--charset",
			"-c" }, description = "The character set to use when writing the output file (default: ${DEFAULT-VALUE})")
	private Charset charset = Charset.defaultCharset();

	@Option(names = { "--author",
			"-a" }, description = "The default author to use if one is not specified for an entry.")
	private String defaultAuthor;

	@Option(names = { "--pretty-print", "-p" }, description = "Pretty print all json files.")
	private boolean prettyPrint;

	@Option(names = { "--recursive", "-r" }, description = "Searches all subdirectories.")
	private boolean recursive;

	@Option(names = { "--tag-separator",
			"-t" }, description = "The character to use to separate tags.  Defaults to the space character.")
	private char tagSeparator = ' ';

	@Option(names = { "--time-zone",
			"-z" }, description = "The time zone to use when parsing date/times in file names (default: ${DEFAULT-VALUE})")
	private ZoneId zone = ZoneId.systemDefault();

	private Gson gson;

	private void writeJson(File file, Object obj) throws IOException {
		FileWriter fw = new FileWriter(file, charset);
		gson.toJson(obj, fw);
		fw.close();
	}

	private void processDir(File dir, int baseDirLength) throws Exception {
		List<Entry> entries = new ArrayList<>();
		for (File file : FileUtils.listFiles(dir, extensions, recursive)) {
			if (!file.isHidden()) {
				Entry entry = Entry.parse(file, tagSeparator, TimeZone.getTimeZone(zone), baseDirLength);
				if (entry.getAuthor() == null && defaultAuthor != null) {
					entry.setAuthor(defaultAuthor);
				}

				entries.add(entry);
			}
		}

		if (!entries.isEmpty()) {
			entries.sort(null); // sort by natural order, aka timestamp
			writeJson(new File(dir, fileName), entries);
		}
	}

	@Override
	public Integer call() throws Exception {
		GsonBuilder builder = new GsonBuilder();
		if (prettyPrint) {
			builder.setPrettyPrinting();
		}
		gson = builder.create();

		for (Path path : paths) {
			processDir(path.toFile(), path.toAbsolutePath().toString().length());
		}
		return 0;
	}

	public static void main(String... args) {
		System.exit(new CommandLine(new NefTransform()).execute(args));
	}
}
