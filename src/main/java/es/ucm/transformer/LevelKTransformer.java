package es.ucm.transformer;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.antlr.v4.runtime.Token;

import es.ucm.javaparser.Java8BaseListener;
import es.ucm.javaparser.Java8Lexer;
import es.ucm.javaparser.Java8Parser.AssertStatementContext;
import es.ucm.javaparser.Java8Parser.AssignmentContext;
import es.ucm.javaparser.Java8Parser.ExpressionContext;
import es.ucm.javaparser.Java8Parser.LeftHandSideContext;
import es.ucm.javaparser.Java8Parser.LocalVariableDeclarationContext;
import es.ucm.javaparser.Java8Parser.LocalVariableDeclarationStatementContext;
import es.ucm.javaparser.Java8Parser.MethodDeclarationContext;
import es.ucm.javaparser.Java8Parser.MethodInvocationContext;
import es.ucm.javaparser.Java8Parser.MethodInvocation_lf_primaryContext;
import es.ucm.javaparser.Java8Parser.MethodInvocation_lfno_primaryContext;
import es.ucm.javaparser.Java8Parser.ReturnStatementContext;
import es.ucm.javaparser.Java8Parser.StatementContext;
import es.ucm.javaparser.Java8Parser.VariableDeclaratorIdContext;

public class LevelKTransformer extends Java8BaseListener {
	private static final String IGNORED_PREFIX = "_option_";
	
	
	private List<Modification> modifications;
	private String currentMethodName;
	
	private int assertionNumber;
	private int callNumber;
	private int freshNameGenerator;
	
	/*
	 * We distinguish between two sets of function names:
	 * 
	 * L0: represented by zeroLevelFunNames
	 * LL: represented by lastLevelFunNames
	 *  
	 * It always holds that L0 \subseteq LL
	 * 
	 * The following transformation is applied to the functions belonging to
	 * any of these sets (i.e. the members of LL).
	 * 
	 *   - The return type is changed to Maybe
	 *   - Returns are transformed into Maybe.createValue
	 *   - Calls to functions in LL are transformed.
	 *   
	 * For the function names in L0, the following transformation is also applied:
	 *   - assert statements transformed into if(...) Maybe.generateError
	 * 
	 */
	
	private Set<String> zeroLevelFunNames;
	private Set<String> lastLevelFunNames;
	
	
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

	
	private String maybeName;
	private boolean activeTransformation;
	private LocalVariableDeclarationStatementContext declarationStatementContext;
	private AssignmentContext assignmentContext;
	private StatementContext statementContext;
	
	
	public LevelKTransformer(String maybeName, 
			Set<String> zeroLevelFunNames,
			Set<String> lastLevelFunNames,
			Map<String, PositionLength> originalEnvironment,
			Map<String, String> transformedEnvironment) {
		super();
		this.maybeName = maybeName;
		this.zeroLevelFunNames = zeroLevelFunNames;
		this.lastLevelFunNames = lastLevelFunNames;
		this.modifications = new LinkedList<>();
		this.activeTransformation = false;
		this.originalEnvironment = originalEnvironment;
		this.transformedEnvironment = transformedEnvironment;
	}
	
	
	public List<Modification> getModifications() {
		return modifications;
	}
	
	@Override
	public void enterMethodDeclaration(MethodDeclarationContext ctx) {
		assertionNumber = 1;
		callNumber = 1;
		freshNameGenerator = 1;
		
		Token methodNameToken = ctx.methodHeader().methodDeclarator().Identifier().getSymbol();
		currentMethodName = methodNameToken.getText();
		
		if (lastLevelFunNames.contains(currentMethodName)) {
			activeTransformation = true;
		} else {
			activeTransformation = false;
		}
		
	}

	
	
	@Override
	public void exitMethodDeclaration(MethodDeclarationContext ctx) {
		currentMethodName = null;
		activeTransformation = false;
	}
	
	@Override
	public void enterLocalVariableDeclarationStatement(
			LocalVariableDeclarationStatementContext ctx) {
		this.declarationStatementContext = ctx;
	}
	
	@Override
	public void exitLocalVariableDeclarationStatement(
			LocalVariableDeclarationStatementContext ctx) {
		this.declarationStatementContext = null;
	}
	
	@Override
	public void enterAssignment(AssignmentContext ctx) {
		this.assignmentContext = ctx;
	}
	
	@Override
	public void exitAssignment(AssignmentContext ctx) {
		this.assignmentContext = null;
	}
	
	@Override
	public void enterStatement(StatementContext ctx) {
		this.statementContext = ctx;
	}
	
	public void exitStatement(StatementContext ctx) {
		this.statementContext = null;
	};
	
	@Override
	public void enterMethodInvocation(MethodInvocationContext ctx) {
		int startPosition = ctx.getStart().getStartIndex();
		int endPosition = 0;
		if (statementContext != null) {
			endPosition = statementContext.getStop().getStopIndex();
		} else if (declarationStatementContext != null) {
			endPosition = declarationStatementContext.getStop().getStopIndex();
		}
		int positionAfterName;
		String methodName = null;
		if (ctx.methodName() != null) {
			methodName = ctx.methodName().Identifier().getSymbol().getText();
			positionAfterName = ctx.methodName().Identifier().getSymbol().getStopIndex() + 1;
		} else {
			methodName = ctx.Identifier().getSymbol().getText();
			positionAfterName = ctx.Identifier().getSymbol().getStopIndex() + 1;
		}
		
		handleMethodCall(startPosition, endPosition, positionAfterName,
				methodName);
	}
	
	@Override
	public void enterMethodInvocation_lf_primary(
			MethodInvocation_lf_primaryContext ctx) {
		int startPosition = ctx.getStart().getStartIndex();
		int endPosition = 0;
		if (statementContext != null) {
			endPosition = statementContext.getStop().getStopIndex();
		} else if (declarationStatementContext != null) {
			endPosition = declarationStatementContext.getStop().getStopIndex();
		}
		int positionAfterName = ctx.Identifier().getSymbol().getStopIndex() + 1;
		String methodName = ctx.Identifier().getSymbol().getText();
			
		handleMethodCall(startPosition, endPosition, positionAfterName,
				methodName);
	}
	
	@Override
	public void enterMethodInvocation_lfno_primary(
			MethodInvocation_lfno_primaryContext ctx) {
		int startPosition = ctx.getStart().getStartIndex();
		int endPosition = 0;
		if (statementContext != null) {
			endPosition = statementContext.getStop().getStopIndex();
		} else if (declarationStatementContext != null) {
			endPosition = declarationStatementContext.getStop().getStopIndex();
		}
		int positionAfterName;
		String methodName = null;
		if (ctx.methodName() != null) {
			methodName = ctx.methodName().Identifier().getSymbol().getText();
			positionAfterName = ctx.methodName().Identifier().getSymbol().getStopIndex() + 1;
		} else {
			methodName = ctx.Identifier().getSymbol().getText();
			positionAfterName = ctx.Identifier().getSymbol().getStopIndex() + 1;
		}
		
		handleMethodCall(startPosition, endPosition, positionAfterName,
				methodName);
	}


	private void handleMethodCall(int startPosition, int endPosition,
			int positionAfterName, String methodName) {
		if (activeTransformation && lastLevelFunNames.contains(methodName)) {
			if (declarationStatementContext != null) {
				LocalVariableDeclarationContext declarationContext = declarationStatementContext.localVariableDeclaration();
				
				int localDeclEndPosition =  declarationStatementContext.getStop().getStopIndex();
				String transformedType = transformedEnvironment.get(methodName);
				
				int startType = declarationContext.unannType().getStart().getStartIndex();
				int endType = declarationContext.unannType().getStop().getStopIndex();
				
				if (transformedType != null) {
					modifications.add(new Removal(startType, endType - startType + 1));
					modifications.add(new Insertion(startType, transformedType + " "));
				} else {
					modifications.add(new Insertion(startType, maybeName + "<"));
					modifications.add(new Insertion(endType + 1, ">"));
				}
				
				VariableDeclaratorIdContext vdCtx =  
						declarationContext.variableDeclaratorList().variableDeclarator(0).variableDeclaratorId();
				int vdStart = vdCtx.getStart().getStartIndex();
				int vdEnd = vdCtx.getStop().getStopIndex();
				
				modifications.add(new Removal(vdStart, vdEnd - vdStart + 1));
				String dummyName = getFreshDummyName();
				modifications.add(new Insertion(vdStart, dummyName));
				modifications.add(new Insertion(positionAfterName, "Copy"));
				modifications.add(new Insertion(localDeclEndPosition + 1, "\n\t\t"));
				modifications.add(new CopyFromOriginal(localDeclEndPosition + 1, startType, endType - startType + 1));
				modifications.add(new Insertion(localDeclEndPosition + 1, " "));
				modifications.add(new CopyFromOriginal(localDeclEndPosition + 1, vdStart, vdEnd - vdStart + 1));
				modifications.add(new Insertion(localDeclEndPosition + 1, ";"));
				modifications.add(new Insertion(localDeclEndPosition + 1, "\n\t\tif (" + dummyName + ".isValue()) { "));
				modifications.add(new CopyFromOriginal(localDeclEndPosition + 1, vdStart, vdEnd - vdStart + 1));
				modifications.add(new Insertion(localDeclEndPosition + 1, " = " + dummyName + ".getValue(); } else { " + maybeName 
						+ ".propagateError(\"" + currentMethodName + "\", " + callNumber + ", " + dummyName + "); }"
						));
				

				
				
				
				
			} else if (assignmentContext != null) {
				
				LeftHandSideContext lhsCtx = assignmentContext.leftHandSide();
				int startLhs = lhsCtx.getStart().getStartIndex();
				int endLhs = lhsCtx.getStop().getStopIndex();
				
				PositionLength originalTypePos = originalEnvironment.get(methodName);
				String transformedType = transformedEnvironment.get(methodName);
				if (transformedType != null) {
					modifications.add(new Insertion(startLhs, transformedType + " "));
				} else {
					modifications.add(new Insertion(startLhs, maybeName + "<"));
					modifications.add(new CopyFromOriginal(startLhs, originalTypePos.getPosition(), originalTypePos.getLength()));
					modifications.add(new Insertion(startLhs, "> "));
				}
				
				modifications.add(new Removal(startLhs, endLhs - startLhs + 1));
				String dummyVarName = getFreshDummyName();
				modifications.add(new Insertion(startLhs, dummyVarName));
				modifications.add(new Insertion(positionAfterName, "Copy"));
				modifications.add(new Insertion(endPosition + 1, "\n\t\t if(" + dummyVarName + ".isValue()) { "));
				modifications.add(new CopyFromOriginal(endPosition + 1, startLhs, endLhs - startLhs + 1));
				modifications.add(new Insertion(endPosition + 1, " = " + dummyVarName + ".getValue(); } else { " + maybeName 
						+ ".propagateError(\"" + currentMethodName + "\", " + callNumber + ", " + dummyVarName + "); }"));
				
				
				
			} else {
				PositionLength originalTypePos = originalEnvironment.get(methodName);
				String transformedType = transformedEnvironment.get(methodName);
				
				if (transformedType != null) {
					modifications.add(new Insertion(startPosition, transformedType + " "));
				} else {
					modifications.add(new Insertion(startPosition, maybeName + "<"));
					modifications.add(new CopyFromOriginal(startPosition, originalTypePos.getPosition(), originalTypePos.getLength()));
					modifications.add(new Insertion(startPosition, "> "));
				}
				String dummyVarName = getFreshDummyName();
				modifications.add(new Insertion(startPosition, dummyVarName + " = "));
				modifications.add(new Insertion(positionAfterName, "Copy"));
				modifications.add(new Insertion(endPosition + 1, "\n\t\tif(!" + dummyVarName + ".isValue()) { return " +
						maybeName + ".propagateError(\"" + currentMethodName + "\", " + callNumber + ", " +
						dummyVarName + "); }"						
				));
			}
		}
		callNumber++;
	}


	private String getFreshDummyName() {
		return IGNORED_PREFIX + freshNameGenerator++;
	}
	
	
	@Override
	public void enterReturnStatement(ReturnStatementContext ctx) {
		if (!activeTransformation) return;
		ExpressionContext ectx = ctx.expression();
		if (ectx != null) {
			modifications.add(new Insertion(ectx.getStart().getStartIndex(), maybeName + ".createValue("));
			modifications.add(new Insertion(ectx.getStop().getStopIndex() + 1, ")"));
		}
	}
	
	@Override
	public void enterAssertStatement(AssertStatementContext ctx) {
		if (!activeTransformation || !zeroLevelFunNames.contains(currentMethodName)) return;
		Token assertToken = ctx.getToken(Java8Lexer.ASSERT, 0).getSymbol();
		int assertPosition = assertToken.getStartIndex();
		int assertLength = assertToken.getStopIndex() - assertPosition + 1;
		modifications.add(new Removal(assertPosition, assertLength));
		
		ExpressionContext ectx = ctx.expression(0);
		modifications.add(new Insertion(ectx.getStart().getStartIndex(), "if (!("));
		modifications.add(new Insertion(ectx.getStop().getStopIndex() + 1, ")) { return " + maybeName + ".generateError(\"" +
				currentMethodName + "\", " + assertionNumber + ", "));
		
		Token semicolonToken = ctx.getToken(Java8Lexer.SEMI, 0).getSymbol();
		
		if (ctx.expression().size() > 1) {
			Token colonToken = ctx.getToken(Java8Lexer.COLON, 0).getSymbol();
			modifications.add(new Removal(colonToken.getStartIndex(), 1));			
		} else {
			modifications.add(new Insertion(semicolonToken.getStartIndex(), "\"\""));
		}
		
		modifications.add(new Removal(semicolonToken.getStartIndex(), 1));
		modifications.add(new Insertion(semicolonToken.getStartIndex(), "); }"));
		
		assertionNumber++;
	}
	
	
	
	
}
