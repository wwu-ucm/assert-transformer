package es.ucm.transformer;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.IOUtils;

import es.ucm.javaparser.Java8Lexer;
import es.ucm.javaparser.Java8Parser;

public class Main {
	public static void process(Reader reader, 
			Writer writer, 
			int maxLevel,
			String maybeName, 
			Set<String> funNames, 
			Set<String> preservedOriginalNames) throws IOException {
		String fileContents = IOUtils.toString(reader);

		ANTLRInputStream inputStream = new ANTLRInputStream(new StringReader(fileContents));
		Java8Lexer lexer = new Java8Lexer(inputStream);
		CommonTokenStream tokenStream = new CommonTokenStream(lexer);
		Java8Parser parser = new Java8Parser(tokenStream);
		ParseTree tree = parser.compilationUnit();
		ParseTreeWalker walker = new ParseTreeWalker();
		
		DependencyGraphGenerator dgGen = getDependencyGraph(tree,
				walker);
		
		
		Set<String> zeroLevelFunctions = new HashSet<String>();
		if (funNames != null) {
			zeroLevelFunctions.addAll(funNames);
		} else {
			zeroLevelFunctions.addAll(dgGen.getMethodsWithAssertions());
		}
		
		Set<String> lastLevelFunctions = getLastLevelFunctions(maxLevel,
				dgGen.getDependencyGraph(), zeroLevelFunctions);

		EnvironmentGenerator envGen = getTypeEnvironment(maybeName, tree, walker,
				lastLevelFunctions, preservedOriginalNames);
		
		LevelKTransformer transformer = new LevelKTransformer(maybeName, zeroLevelFunctions,
				lastLevelFunctions, envGen.getOriginalEnvironment(), envGen.getTransformedEnvironment());
		walker.walk(transformer, tree);
		

		transformer.getModifications().addAll(envGen.getModifications());
		
		Reader secondReader = new StringReader(fileContents);
		List<Modification> modificationList = transformer.getModifications(); 
		TextFileModifier.modify(modificationList, secondReader, writer, fileContents);
		writer.flush();
		writer.close();
	}

	private static EnvironmentGenerator getTypeEnvironment(String maybeName,
			ParseTree tree, ParseTreeWalker walker,
			Set<String> lastLevelFunctions, Set<String> preserveOriginalFunctions) {
		EnvironmentGenerator envGen = new EnvironmentGenerator(maybeName, lastLevelFunctions, preserveOriginalFunctions);
		walker.walk(envGen, tree);
		return envGen;
	}

	private static Set<String> getLastLevelFunctions(int maxLevel,
			Map<String, List<String>> dependencyGraph,
			Set<String> zeroLevelFunctions) {
		Set<String> lastLevelFunctions = new HashSet<String>();
		lastLevelFunctions.addAll(zeroLevelFunctions);
		
		List<String> newFunctions = new LinkedList<String>();
		newFunctions.addAll(zeroLevelFunctions);
		
		for (int level = maxLevel; level > 0; level--) {
			List<String> nextNewFunctions = new LinkedList<>();
			for (String funName : newFunctions) {
				for (String caller : dependencyGraph.get(funName)) {
					if (!lastLevelFunctions.contains(caller)) {
						lastLevelFunctions.add(caller);
						nextNewFunctions.add(caller);
					}
				}
			}
			newFunctions = nextNewFunctions;
		}
		return lastLevelFunctions;
	}

	private static DependencyGraphGenerator getDependencyGraph(ParseTree tree,
			ParseTreeWalker walker) {
		DependencyGraphGenerator dgGen = new DependencyGraphGenerator();
		walker.walk(dgGen, tree);
		return dgGen;
	}

	public static void showUsage(Options options) {
		HelpFormatter hf = new HelpFormatter();
		hf.printHelp("assertTransformer input.java [options]", options);
		System.out.println("\nIf no output file is specified, standard output will be used.");
	}

	public static void main(String[] args) {
		Options options = configureCLIOptions();
		
		CommandLineParser clp = new BasicParser();
		try {
			CommandLine cl = clp.parse(options, args);
			String[] input = cl.getArgs();
			if (input.length < 1) {
				System.out.println("Error: no input file specified");
				showUsage(options);
			} else if (input.length > 1) {
				System.out.println("Error: too many input files specified");
				showUsage(options);				
			} else {
				String fileName = input[0];
				Reader rd = new BufferedReader(new FileReader(fileName));
				Writer pw = new OutputStreamWriter(System.out);
				if (cl.hasOption('o')) {
					pw = new PrintWriter(new FileWriter(cl.getOptionValue('o')));
				}
				String maybeName = "MayBe";
				if (cl.hasOption('m')) {
					maybeName = cl.getOptionValue('m');
				}
				
				int level = 0;
				if (cl.hasOption('l')) {
					try {
						level = Integer.parseInt(cl.getOptionValue('l'));
					} catch (NumberFormatException e) {
						rd.close();
						throw new ParseException("Invalid level :" + cl.getOptionValue('l'));
					}
				}
				
				String[] funs = null;
				if (cl.hasOption('f')) {
					String funNames = cl.getOptionValue('f');
					funs = funNames.split(",");
				}
				
				
				String[] preserved = new String[] {};
				if (cl.hasOption('p')) {
					String preserveNames = cl.getOptionValue('p');
					if (preserveNames.equals("*")) {
						preserved = null;
					} else {
						preserved = preserveNames.split(",");
					}
				}
				
				process(rd, pw, level, maybeName, 
						funs != null ? new HashSet<String>(Arrays.asList(funs)) : null,
						preserved != null ? new HashSet<String>(Arrays.asList(preserved)) : null);
			}
		} catch (ParseException e) {
			System.out.println("Error: " + e.getMessage());
			showUsage(options);			
		} catch (FileNotFoundException e) {
			System.out.println("Error: " + e.getMessage());
		} catch (IOException e) {
			System.out.println("Error: " + e.getMessage());
		}
		
	}

	private static Options configureCLIOptions() {
		Options options = new Options();
		Option levelOption = new Option("l", "level", true, "Maximum level (default: 0)");
		levelOption.setArgName("level");
		options.addOption(levelOption);
		Option output = new Option("o", null, true, "Output file");
		output.setArgName("file");
		options.addOption(output);
		Option maybeNameOpt = new Option("m", "maybe", true, "Maybe class name (default: \"MayBe\")");
		maybeNameOpt.setArgName("className");
		options.addOption(maybeNameOpt);
		Option funNamesOpt = new Option("f", null, true, "Target functions (separated by commas; default: functions containing an 'assert')");
		funNamesOpt.setArgName("names");
		options.addOption(funNamesOpt);
		Option preserveOption = new Option("p", "preserve-originals", true, "Functions whose original version will be kept in the output (separated by commas). Use '*' (between quotes) to preserve all.");
		preserveOption.setArgName("names");
		options.addOption(preserveOption);
		return options;
	}

	
}
