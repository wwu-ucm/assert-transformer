package es.ucm.transformer;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.antlr.v4.runtime.Token;

import es.ucm.javaparser.Java8BaseListener;
import es.ucm.javaparser.Java8Lexer;
import es.ucm.javaparser.Java8Parser.MethodDeclarationContext;
import es.ucm.javaparser.Java8Parser.UnannTypeContext;

public class EnvironmentGenerator extends Java8BaseListener {
	private Set<String> lastLevelFunNames;
	private Set<String> preserveOriginalFunNames;
	private String maybeName;
	private List<Modification> modifications;

	
	/*
	 * It stores, for every function symbol being transformed, its type in the
	 * original version. It is given in a (position, length) pair,
	 * referring to a fragment of the original program.
	 * 
	 * If a function being transformed does not belong to this map,
	 * then its type in the original program is void
	 */
	private Map<String, PositionLength> originalEnvironment;
	
	/*
	 * It stores, for every function symbol being transformed, its type in the
	 * transformed version if it is of a basic type. If a function being transformed
	 * does not belong to this map, then its type in the original program is
	 * "Maybe<" + typeOriginalProgram + ">"
	 */
	private Map<String, String> transformedEnvironment;

	public EnvironmentGenerator(String maybeName, Set<String> lastLevelFunNames, Set<String> preserveOriginalFunNames) {
		this.maybeName = maybeName;
		this.lastLevelFunNames = lastLevelFunNames;
		this.preserveOriginalFunNames = preserveOriginalFunNames;
		this.modifications = new LinkedList<>();
		this.originalEnvironment = new HashMap<>();
		this.transformedEnvironment = new HashMap<>();
	}
	
	public Map<String, PositionLength> getOriginalEnvironment() {
		return originalEnvironment;
	}
	
	public Map<String, String> getTransformedEnvironment() {
		return transformedEnvironment;
	}
	
	public List<Modification> getModifications() {
		return modifications;
	}
	
	@Override
	public void enterMethodDeclaration(MethodDeclarationContext ctx) {
		Token methodNameToken = ctx.methodHeader().methodDeclarator().Identifier().getSymbol();
		String currentMethodName = methodNameToken.getText();
		
		if (lastLevelFunNames.contains(currentMethodName)) {
		
			UnannTypeContext typeCtx = ctx.methodHeader().result().unannType(); 
			changeResultType(currentMethodName, ctx, typeCtx);
			
			if (typeCtx == null) {
				// The result type of the function is void
				addReturnNullStatement(ctx);
			}
	
			modifications.add(new Insertion(methodNameToken.getStopIndex() + 1, "Copy"));
			if (preserveOriginalFunNames == null || preserveOriginalFunNames.contains(currentMethodName)) {
				modifications.add(new Insertion(ctx.getStop().getStopIndex() + 1, "\n\n\t"));
				modifications.add(new CopyFromOriginal(ctx.getStop().getStopIndex() + 1, 
						ctx.getStart().getStartIndex(),
						ctx.getStop().getStopIndex() - ctx.getStart().getStartIndex() + 1));
			}
			
		} 
	}

	private void changeResultType(String funName, MethodDeclarationContext ctx,
			UnannTypeContext typeCtx) {
		if (typeCtx != null) {
			if (typeCtx.unannPrimitiveType() != null) {
				// Return type is a basic type
				Token basicType = typeCtx.unannPrimitiveType().getStart();
				modifications.add(new Removal(basicType.getStartIndex(), basicType.getStopIndex() - basicType.getStartIndex() + 1));
				String typeName = getTypeName(basicType);
				String transformedTypeName = maybeName + "<" + typeName + ">";
				modifications.add(new Insertion(basicType.getStartIndex(), transformedTypeName));
				originalEnvironment.put(funName, new PositionLength(basicType.getStartIndex(), 
						basicType.getStopIndex() - basicType.getStartIndex() + 1));
				transformedEnvironment.put(funName, transformedTypeName);
			} else {
				// Return type is not a basic type
				modifications.add(new Insertion(typeCtx.getStart().getStartIndex(), maybeName + "<"));
				modifications.add(new Insertion(typeCtx.getStop().getStopIndex() + 1, ">"));
				originalEnvironment.put(funName, new PositionLength(typeCtx.getStart().getStartIndex(),
						typeCtx.getStop().getStopIndex() - typeCtx.getStart().getStartIndex() + 1));
			}
		} else {
			// Function returns 'void'
			modifications.add(new Removal(ctx.methodHeader().result().getStart().getStartIndex(), 
					ctx.methodHeader().result().getStop().getStopIndex() - ctx.methodHeader().result().getStart().getStartIndex() + 1));
			String transformedTypeName = maybeName + "<Void>";
			modifications.add(new Insertion(ctx.methodHeader().result().getStart().getStartIndex(), transformedTypeName));
			transformedEnvironment.put(funName, transformedTypeName);
		}
	}
	
	private String getTypeName(Token basicType) {
		String typeName = null;
		switch (basicType.getType()) {
		case Java8Lexer.INT:
			typeName = "Integer";
			break;
		case Java8Lexer.FLOAT:
			typeName = "Float";
			break;
		case Java8Lexer.DOUBLE:
			typeName = "Double";
			break;
		case Java8Lexer.LONG:
			typeName = "Long";
			break;
		case Java8Lexer.BYTE:
			typeName = "Byte";
			break;
		case Java8Lexer.SHORT:
			typeName = "Short";
			break;
		case Java8Lexer.BOOLEAN:
			typeName = "Boolean";
			break;
		case Java8Lexer.CHAR:
			typeName = "Character";
			break;
		default:
			System.out.println("Unknown basic type: " + basicType);
		}
		return typeName;
	}

	private void addReturnNullStatement(MethodDeclarationContext ctx) {
		if (ctx.methodBody().block() != null) {
			Token rbrace = ctx.methodBody().block().getToken(Java8Lexer.RBRACE, 0).getSymbol();
			modifications.add(new Insertion(rbrace.getStartIndex(), "\treturn " + maybeName + ".createValue(null);\n\t"));
		}
	}
}
