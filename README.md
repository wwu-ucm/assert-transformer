# assert-transformer

In order to compile the code you need Eclipse and/or Maven, and Java 8.

To compile, type the following in the root folder (i.e. that containing pom.xml):

        mvn compile

To generate the .jar file:

        mvn package

To generate the .jar file with dependencies:

        mvn assembly:single

## Usage instructions

This tool transforms all the source code files contained within a given folder. 

You have to annotate the methods to be transformed with the `@AssertTransform` annotation. This annotation is defined in the `assert-transformer-tools` project. An output folder will be generated with the transformed versions of the input folder. These versions make use of several classes (e.g. `Maybe`, `ResultContainer`, etc) which are implemented in `assert-transformer-tools`.

	Usage: assert-transformer input_folder [options]
	 -l,--level <level>      Maximum level (default: infinity)
	 -o,--output <file>      Output directory (default: "output")
	 -r,--remove-originals   Delete original methods, i.e. keep only
				 transformed methods

Usage example:

	java -jar assert-transformer.jar assert-transformer-test/src
