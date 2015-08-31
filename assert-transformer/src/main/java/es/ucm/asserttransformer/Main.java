package es.ucm.asserttransformer;

import java.util.Arrays;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import spoon.Launcher;
import spoon.OutputType;
import spoon.SpoonException;


public class Main {
    
    private static Options configureCLIOptions() {
            Options options = new Options();
            Option levelOption = new Option("l", "level", true, "Maximum level (default: infinity)");
            levelOption.setArgName("level");
            options.addOption(levelOption);
            Option output = new Option("o", "output", true, "Output directory (default: \"output\")");
            output.setArgName("file");
            options.addOption(output);
            Option preserveOption = new Option("r", "remove-originals", false, "Delete original methods, i.e. keep only transformed methods");
            options.addOption(preserveOption);
            Option allAssertions = new Option("a", "all-assertions", false, "Transform all the methods containing an assertion, "
                    + "without regard to the @AssertTransform annotation");
            options.addOption(allAssertions);
            return options;
    }

    public static void main(String[] args) {
        Options options = configureCLIOptions();
        CommandLineParser clp = new DefaultParser();
        try {
            CommandLine cl = clp.parse(options, args);
            String input[] = cl.getArgs();
            if (input.length == 0) {
                throw new SourceFolderException("No source folder specified");
            } else if (input.length > 1) {
                throw new SourceFolderException("Too many source folders specified");
            }
            Globals.maxLevel = Integer.valueOf(cl.getOptionValue('l', "1000"));
            String outputDir = cl.getOptionValue('o', "./output");
            Globals.removeOriginals = cl.hasOption('r');

            Launcher spoon = new Launcher();
            spoon.addInputResource(input[0]);
            spoon.setOutputDirectory(outputDir);
            if (!spoon.getModelBuilder().build()) {
                System.out.println("Source code contains compilation errors");
                return;
            }
        
            spoon.getEnvironment().setAutoImports(true);
            
            if (cl.hasOption("a")) {
                spoon.getModelBuilder().process(
                        Arrays.asList("es.ucm.asserttransformer.processors.AnnotateMethodsWithAssertions")
                );
            }
            
            spoon.getModelBuilder().process(Arrays.asList(
                    "es.ucm.asserttransformer.processors.AnnotateMethodsL0"
            ));
            
            int level = 0;
            do {
                Globals.numTaggedMethods = 0;
                spoon.getModelBuilder().process(Arrays.asList(
                        "es.ucm.asserttransformer.processors.AnnotateMethodsLk"
                ));
                level++;
            } while (Globals.numTaggedMethods > 0 &&  level < Globals.maxLevel);
            spoon.getModelBuilder().process(Arrays.asList(
                    "es.ucm.asserttransformer.processors.CopyAndTransformMethods",
                    "es.ucm.asserttransformer.processors.RemoveAuxiliaryAnnotations"
            ));
            spoon.getModelBuilder().generateProcessedSourceFiles(OutputType.COMPILATION_UNITS);
        } catch (ParseException | SourceFolderException | NumberFormatException e) {
            System.err.println("Error: " + e.getMessage());
            HelpFormatter hf = new HelpFormatter();
            hf.printHelp("assert-transformer input_folder [options]", options);
        } catch (SpoonException e) {
            System.err.println("Spoon exception: " + e.getMessage());
        }
    }
}
