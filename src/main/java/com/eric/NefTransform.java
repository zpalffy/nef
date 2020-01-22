package com.eric;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.Callable;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.commons.io.FileUtils;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "nef", mixinStandardHelpOptions = true, version = { "@|bold,magenta nef|@ 1.0",
		"@|underline https://github.com/zpalffy/nef|@" }, description = "Utility to generate json metadata for entries.")
public class NefTransform implements Callable<Integer> {

	@Option(names = { "--directory",
			"-d" }, description = "The directory to consider for entries.  Defaults to current.")
	private Path directory = Paths.get(".");

	@Option(names = { "--entries",
			"-e" }, description = "Write the entries.json file.  If not used, this is written to sysout.")
	private boolean writeJson;

	@Option(names = "--extensions", description = "The file extensions to consider when creating entries.  Note: do not include the '.' character (default: ${DEFAULT-VALUE})")
	private String[] extensions = new String[] { "md", "markdown", "txt" };

	@Option(names = { "--author",
			"-a" }, description = "The default author to use if one is not specified for an entry.")
	private String defaultAuthor;

	@Option(names = { "--pretty-print", "-p" }, description = "Pretty print json.")
	private boolean prettyPrint;

	@Option(names = { "--recursive", "-r" }, description = "Searches all subdirectories.")
	private boolean recursive;

	@Option(names = { "--tag-separator",
			"-t" }, description = "The character to use to separate tags.  Defaults to the space character.")
	private char tagSeparator = ' ';

	@Option(names = { "--time-zone",
			"-z" }, description = "The time zone to use when parsing date/times in file names (default: ${DEFAULT-VALUE})")
	private ZoneId timezone = ZoneId.systemDefault();

	@Option(names = { "--charset",
			"-c" }, description = "The character set to use when reading and writing files. (default: ${DEFAULT-VALUE})")
	private Charset charset = Charset.defaultCharset();

	@Option(names = { "--index",
			"-i" }, description = "Builds a lunr search index over the entries and their contents and stores the results.  Beware this could significantly slow down the command as each entry is loaded and indexed.  See lunrjs.com")
	private boolean index;

	private Gson gson;

	private void processDir(File dir, int baseDirLength) throws Exception {
		List<Entry> entries = new ArrayList<>();
		for (File file : FileUtils.listFiles(dir, extensions, recursive)) {
			if (!file.isHidden() && file.length() > 0) {
				Entry entry = Entry.parse(file, tagSeparator, TimeZone.getTimeZone(timezone), baseDirLength);
				if (entry.getAuthor() == null && defaultAuthor != null) {
					entry.setAuthor(defaultAuthor);
				}

				entries.add(entry);

			}
		}

		if (!entries.isEmpty()) {
			entries.sort(null); // sort by natural order, aka timestamp

			if (writeJson) {
				try (BufferedWriter br = Files.newBufferedWriter(directory.resolve("entries.json"), charset)) {
					gson.toJson(entries, br);
				}
			} else {
				gson.toJson(entries, System.out);
			}

			// FileUtils.writeStringToFile(directory.resolve("entries.json").toFile(),
			// gson.toJson(entries), charset);

			if (index) {
				buildIndex(entries);
			}
		} else {
			System.err.println("No entries were found in " + dir);
		}
	}

	private void buildIndex(List<Entry> entries)
			throws MalformedURLException, ScriptException, IOException, NoSuchMethodException {

		ScriptEngine js = new ScriptEngineManager().getEngineByExtension("js");
		js.eval(new InputStreamReader(getClass().getResourceAsStream("/index.js")));

		Object retVal = ((Invocable) js).invokeFunction("index", entries, prettyPrint);
		FileUtils.writeStringToFile(directory.resolve("lunr-index.json").toFile(), retVal.toString(), charset);
	}

	@Override
	public Integer call() throws Exception {
		GsonBuilder builder = new GsonBuilder();
		if (prettyPrint) {
			builder.setPrettyPrinting();
		}
		gson = builder.create();
		Entry.CHARSET = charset;

		processDir(directory.toFile(), directory.toAbsolutePath().toString().length());
		return 0;
	}

	public static void main(String... args) {
		System.exit(new CommandLine(new NefTransform()).execute(args));
	}
}
