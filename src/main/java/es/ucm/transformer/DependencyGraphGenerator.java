package es.ucm.transformer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apache.commons.io.IOUtils;

import es.ucm.javaparser.Java8BaseListener;
import es.ucm.javaparser.Java8Lexer;
import es.ucm.javaparser.Java8Parser;
import es.ucm.javaparser.Java8Parser.AssertStatementContext;
import es.ucm.javaparser.Java8Parser.MethodDeclarationContext;
import es.ucm.javaparser.Java8Parser.MethodInvocationContext;
import es.ucm.javaparser.Java8Parser.MethodInvocation_lf_primaryContext;
import es.ucm.javaparser.Java8Parser.MethodInvocation_lfno_primaryContext;

public class DependencyGraphGenerator extends Java8BaseListener {
	private Map<String, List<String>> dependencyGraph;
	private Set<String> methodsWithAssertions;
	private String currentMethodName;
	
	public DependencyGraphGenerator() {
		this.dependencyGraph = new HashMap<>();
		this.methodsWithAssertions = new HashSet<>();
	}
	
	public Map<String, List<String>> getDependencyGraph() {
		return dependencyGraph;
	}
	
	public Set<String> getMethodsWithAssertions() {
		return methodsWithAssertions;
	}
	
	@Override
	public void enterMethodDeclaration(MethodDeclarationContext ctx) {
		Token methodNameToken = ctx.methodHeader().methodDeclarator().Identifier().getSymbol();
		currentMethodName = methodNameToken.getText();
	}
	
	@Override
	public void exitMethodDeclaration(MethodDeclarationContext ctx) {
		currentMethodName = null;
	}
	
	@Override
	public void enterMethodInvocation(MethodInvocationContext ctx) {
		if (currentMethodName == null) return;
		String methodName = null;
		if (ctx.methodName() != null) {
			methodName = ctx.methodName().Identifier().getSymbol().getText();
		} else {
			methodName = ctx.Identifier().getSymbol().getText();
		}

		addDependency(methodName);
	}

	private void addDependency(String methodName) {
		List<String> callers = dependencyGraph.getOrDefault(methodName, new LinkedList<>());
		callers.add(currentMethodName);
		dependencyGraph.put(methodName, callers);
	}
	
	@Override
	public void enterMethodInvocation_lf_primary(
			MethodInvocation_lf_primaryContext ctx) {
		if (currentMethodName == null) return;
		String methodName = ctx.Identifier().getSymbol().getText();
		addDependency(methodName);
	}
	
	@Override
	public void enterMethodInvocation_lfno_primary(
			MethodInvocation_lfno_primaryContext ctx) {
		if (currentMethodName == null) return;
		String methodName = null;
		if (ctx.methodName() != null) {
			methodName = ctx.methodName().Identifier().getSymbol().getText();
		} else {
			methodName = ctx.Identifier().getSymbol().getText();
		}

		addDependency(methodName);
	}
	
	@Override
	public void enterAssertStatement(AssertStatementContext ctx) {
		methodsWithAssertions.add(currentMethodName);		
	}

	public static void main(String[] args) throws IOException {
		
		InputStream is = DependencyGraphGenerator.class.getClassLoader().getResourceAsStream("Mia.java");
		String fileContents = IOUtils.toString(is);
		
		
		ANTLRInputStream inputStream = new ANTLRInputStream(new StringReader(fileContents));
		Java8Lexer lexer = new Java8Lexer(inputStream);
		CommonTokenStream tokenStream = new CommonTokenStream(lexer);
		Java8Parser parser = new Java8Parser(tokenStream);
		ParseTree tree = parser.compilationUnit();
		DependencyGraphGenerator dgGen = new DependencyGraphGenerator();
		
		ParseTreeWalker walker = new ParseTreeWalker();
		walker.walk(dgGen, tree);
		
		System.out.println(dgGen.getDependencyGraph());
		System.out.println(dgGen.getMethodsWithAssertions());

		Map<String, List<String>> dg = dgGen.getDependencyGraph();
		
		
		int level = 5;
		
		Set<String> zeroLevelFunctions = new HashSet<String>();
		zeroLevelFunctions.add("f");
		
		Set<String> lastLevelFunctions = new HashSet<String>();
		lastLevelFunctions.addAll(zeroLevelFunctions);
		
		List<String> newFunctions = new LinkedList<String>();
		newFunctions.addAll(zeroLevelFunctions);
		
		while (level > 0) {
			List<String> nextNewFunctions = new LinkedList<>();
			for (String funName : newFunctions) {
				for (String caller : dg.get(funName)) {
					if (!lastLevelFunctions.contains(caller)) {
						lastLevelFunctions.add(caller);
						nextNewFunctions.add(caller);
					}
				}
			}
			newFunctions = nextNewFunctions;
			level--;
		}

		
		EnvironmentGenerator envGen = new EnvironmentGenerator("MayBe", lastLevelFunctions, lastLevelFunctions);
		walker.walk(envGen, tree);
		
		
		System.out.println("Zero level: " + zeroLevelFunctions);
		System.out.println("Final level: " + lastLevelFunctions);
		
		
		System.out.println("Original environment: " + envGen.getOriginalEnvironment());
		System.out.println("Transformed environment: " + envGen.getTransformedEnvironment());
		
		
		LevelKTransformer tr0 = new LevelKTransformer("MayBe", zeroLevelFunctions,
				lastLevelFunctions, envGen.getOriginalEnvironment(), envGen.getTransformedEnvironment());
		walker.walk(tr0, tree);
		

		tr0.getModifications().addAll(envGen.getModifications());
		
		Reader secondReader = new StringReader(fileContents);
		
		List<Modification> modificationList = tr0.getModifications();
		Writer writer = new OutputStreamWriter(System.out);
		TextFileModifier.modify(modificationList, secondReader, 
				writer, fileContents);
		writer.flush();
	}
}
