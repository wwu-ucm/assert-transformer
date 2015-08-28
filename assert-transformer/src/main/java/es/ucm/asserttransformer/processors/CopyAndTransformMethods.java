package es.ucm.asserttransformer.processors;

import es.ucm.asserttransformer.Globals;
import es.ucm.asserttransformer.annotations.AssertStmTransform;
import es.ucm.asserttransformer.annotations.CallTransform;
import es.ucm.asserttransformer.annotations.CopyOfMethod;
import es.ucm.asserttransformer.annotations.MethodTransform;
import es.ucm.asserttransformer.maybe.Maybe;
import es.ucm.asserttransformer.maybe.ResultContainer;
import static java.util.Collections.singletonList;
import static java.util.Arrays.asList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import spoon.processing.AbstractAnnotationProcessor;
import spoon.reflect.code.CtAbstractInvocation;
import spoon.reflect.code.CtAssert;
import spoon.reflect.code.CtAssignment;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtConstructorCall;
import spoon.reflect.code.CtDo;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtFor;
import spoon.reflect.code.CtForEach;
import spoon.reflect.code.CtIf;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtLocalVariable;
import spoon.reflect.code.CtReturn;
import spoon.reflect.code.CtStatement;
import spoon.reflect.code.CtStatementList;
import spoon.reflect.code.CtSwitch;
import spoon.reflect.code.CtSynchronized;
import spoon.reflect.code.CtUnaryOperator;
import spoon.reflect.code.CtWhile;
import spoon.reflect.code.UnaryOperatorKind;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.factory.Factory;
import spoon.reflect.reference.CtTypeReference;

import spoon.reflect.code.CtThrow;
import spoon.reflect.code.CtTry;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.declaration.CtExecutable;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.declaration.CtTypedElement;
import spoon.reflect.declaration.ModifierKind;

public class CopyAndTransformMethods extends AbstractAnnotationProcessor<MethodTransform, CtExecutable<?>> {
    
    private enum InsertDirection {
        BEFORE, AFTER
    }
    
    private class InsertionPoint {
        private final InsertDirection direction;
        private final CtStatement location;

        public InsertionPoint(InsertDirection direction, CtStatement location) {
            this.direction = direction;
            this.location = location;
        }

        public InsertDirection getDirection() {
            return direction;
        }

        public CtStatement getLocation() {
            return location;
        }
        
        public void performInsertion(List<CtStatement> statements) {
            switch (direction) {
                case BEFORE:
                    statements.forEach(location::insertBefore);
                    break;
                case AFTER:
                    for (int i = statements.size() - 1; i >= 0; i--) {
                        location.insertAfter(statements.get(i));
                    }
                    break;
            }
        }
    }

    @Override
    public void process(MethodTransform a, CtExecutable<?> e) {
        System.out.print("Transforming " + e.getSignature() + "...");
        Factory f = getFactory();
        CtTypeReference<Maybe> maybe = f.Type().createReference(Maybe.class);
        
        boolean insideConstructor;
        
        CtClass<?> containerClass = e.getParent(CtClass.class);
        CtTypeReference<ResultContainer> containerType = f.Type().createReference(ResultContainer.class);
        CtParameter<ResultContainer> rcParameter;
        
        CtTypeReference<?> returnTypeOriginal = e.getType();
        CtTypeReference<Maybe> returnTypeCopy = f.Type().createReference(Maybe.class);
        returnTypeCopy.addActualTypeArgument(getHeapType(returnTypeOriginal));
        
        CtExecutable<Maybe> newMethod;
        
        if (e instanceof CtMethod) {
            newMethod = (CtMethod<Maybe>) f.Method().create(containerClass, (CtMethod<?>)e, true);
            newMethod.setType(returnTypeCopy);
            newMethod.setSimpleName(e.getSimpleName() + "Copy");
            
            List<CtReturn<?>> returnStatements = newMethod.getElements((CtReturn<?> x) -> true);
            returnStatements.forEach(
                    (CtReturn<?> exp) -> exp.replace(
                            f.Code().createCodeSnippetStatement(
                                    "return Maybe.createValue(" + exp.getReturnedExpression() + ")"
                            )
                    )
            );
            
            if (e.getType().getQualifiedName().equals("void")) {
                newMethod.getBody().addStatement(
                        f.Code().createCodeSnippetStatement(
                                "return Maybe.createValue(null)"
                        )
                );
            }
            insideConstructor = false;
        } else { // e instanceof CtConstructor
            newMethod = (CtConstructor<Maybe>) f.Constructor().create(containerClass, (CtConstructor<?>) e);
            rcParameter = f.Executable().createParameter(newMethod, containerType, "_rc");

            createFactoryMethod(f, containerClass, e);
            
            insideConstructor = true;
        } 
        
        CtAnnotation<MethodTransform> annNewMethod = newMethod.getAnnotation(
                f.Type().createReference(MethodTransform.class)
        );
        int methodLevel = annNewMethod.getElementValue("level");
        CtAnnotation<CopyOfMethod> copyAnnotation = f.Annotation().annotate(newMethod, CopyOfMethod.class);
        copyAnnotation.addValue("level", methodLevel);

        List<CtAbstractInvocation<?>> calls = newMethod.getAnnotatedChildren(CallTransform.class);
        int index = 0;
        
        while (!calls.isEmpty()) {
            List<CtAbstractInvocation<?>> nextCalls = new LinkedList<>();
            for (CtAbstractInvocation<?> call : calls) {
                
                CtTypeReference<?> returnCallType = ((CtTypedElement<?>)call).getType();
                
                List<CtAbstractInvocation<?>> annotatedChildren = call.getAnnotatedChildren(CallTransform.class);
                annotatedChildren.remove(call);
                if (!annotatedChildren.isEmpty()) {
                    nextCalls.add(call);
                    continue;
                }

                String auxVarName = "_maybe_" + index;
                CtLocalVariable<Maybe> auxVar = createAuxiliaryVariableDeclaration(f, auxVarName, returnCallType);
                newMethod.getBody().insertBegin(auxVar);
                
                List<InsertionPoint> insertionPoints = getInsertionPoints((CtStatement)call);
                
                CtIf conditional = f.Core().createIf();
                conditional.setCondition(
                        f.Code().createCodeSnippetExpression("!" + auxVarName + ".isValue()")
                );
                if (!insideConstructor) {
                    conditional.setThenStatement(
                            f.Code().createCodeSnippetStatement(
                                    "return Maybe.propagateError(\"" + 
                                        e.getSimpleName() + "\", " + index +
                                        ", " + auxVarName + ")"
                            )
                    );
                } else {
                    CtBlock block = f.Core().createBlock();
                    block.addStatement(
                            f.Code().createCodeSnippetStatement(
                                    "_rc.setValue(Maybe.propagateError(\"" + 
                                        e.getSimpleName() + "\", " + index +
                                        ", " + auxVarName + "))"
                            )
                    );
                    block.addStatement(
                            f.Core().createReturn()
                    );
                    conditional.setThenStatement(block);
                }
                

                insertionPoints.forEach(p -> {
                    CtAbstractInvocation<?> invocation = createModifiedInvocation(call, f);
                    p.performInsertion(asList(
                            f.Code().createCodeSnippetStatement(
                                    auxVarName + " = " + invocation.toString()
                            ),
                            conditional
                    ));
                });

                
                
                CtInvocation<?> valueInvoc =
                        createGetValueInvocation(f, auxVar, returnCallType);
                call.replace(valueInvoc);
                index++;
            }
            calls = nextCalls;
        }
        
        List<CtAssert<?>> assertions = newMethod.getAnnotatedChildren(AssertStmTransform.class);
        index = 0;
        for (CtAssert<?> assertion : assertions) {
            CtIf ifStm = f.Core().createIf();
            CtUnaryOperator<Boolean> negCondition = generateNegatedCondition(f, assertion);
            ifStm.setCondition(negCondition);

            if (!insideConstructor) {
                CtStatement generateError = f.Code().createCodeSnippetStatement(
                        "return Maybe.generateError(\"" +
                                e.getSimpleName() + "\", " + index + ", " +
                                assertion.getExpression() +")"
                );
                ifStm.setThenStatement(generateError);
            } else {
                CtBlock block = f.Core().createBlock();
                CtExpression generateError = f.Code().createCodeSnippetExpression(
                        "Maybe.generateError(\"" +
                                e.getSimpleName() + "\", " + index + ", " +
                                assertion.getExpression() +")"
                );
                CtStatement dummySetValue = f.Code().createCodeSnippetStatement(
                        "_rc.setValue(" + generateError + ")"
                );
                
                CtStatement returnStm = f.Core().createReturn();
                block.addStatement(dummySetValue);
                block.addStatement(returnStm);
                ifStm.setThenStatement(block);
            }
            assertion.replace(ifStm);
            index++;
        }
        
        if (e instanceof CtMethod) {
            containerClass.addMethod((CtMethod<Maybe>)newMethod);
            if (Globals.removeOriginals) containerClass.removeMethod((CtMethod<Maybe>)e);
        } else {
            containerClass.addConstructor((CtConstructor) newMethod);
            if (Globals.removeOriginals) containerClass.removeConstructor((CtConstructor)e);
        }
        System.out.println("done");
    }

    private CtTypeReference<?> getHeapType(CtTypeReference<?> type) {
        String name = type.getQualifiedName();
        switch (name) {
            case "int":
                return getFactory().Type().INTEGER;
            case "boolean":
                return getFactory().Type().BOOLEAN;
            case "float":
                return getFactory().Type().FLOAT;
            case "double":
                return getFactory().Type().DOUBLE;
            case "void":
                return getFactory().Type().VOID;
            case "char":
                return getFactory().Type().CHARACTER;
            case "byte":
                return getFactory().Type().BYTE;
            case "long":
                return getFactory().Type().LONG;
            default:
                return type;
        }
    }
    

    
    private void createFactoryMethod(Factory f, CtClass<?> containerClass, CtExecutable<?> e) {
        CtBlock block = f.Core().createBlock();
        CtTypeReference<ResultContainer> typeResC =
                f.Type().createReference(ResultContainer.class);
        typeResC.addActualTypeArgument(containerClass.getReference());
        
        
        CtLocalVariable<ResultContainer> resCDecl = f.Core().createLocalVariable();
        resCDecl.setType(typeResC);
        resCDecl.setSimpleName("_resC");
        CtConstructorCall<ResultContainer> ctCall = f.Core().createConstructorCall();
        ctCall.setType(typeResC);
        ctCall.setArguments(Arrays.asList());
        resCDecl.setDefaultExpression(ctCall);
        block.addStatement(resCDecl);
        
        
        CtLocalVariable varResult = f.Core().createLocalVariable();
        varResult.setType(containerClass.getReference());
        varResult.setSimpleName("_result");
        
        CtConstructorCall ctCallClass = f.Core().createConstructorCall();
        ctCallClass.setType(containerClass.getReference());
        
        List<CtExpression> args = new LinkedList();
        e.getParameters().forEach(par ->
                args.add(f.Code().createVariableRead(par.getReference(), false))
        );
        args.add(f.Code().createVariableRead(resCDecl.getReference(), false));
        ctCallClass.setArguments(args);
        
        varResult.setDefaultExpression(ctCallClass);
        block.addStatement(varResult);
        
        block.addStatement(
                f.Code().createCodeSnippetStatement(
                        "return _resC.orElse(_result)"
                )
        );
        
        CtTypeReference<?> maybeClass = f.Type().createReference(Maybe.class);
        maybeClass.addActualTypeArgument(containerClass.getReference());
        CtMethod<?> factoryMethod = f.Method().create(
                containerClass,
                new HashSet<>(asList(ModifierKind.PUBLIC, ModifierKind.STATIC)),
                maybeClass,
                containerClass.getSimpleName() + "Factory",
                e.getParameters(),
                e.getThrownTypes(),
                block
        );
    }

    private CtUnaryOperator<Boolean> generateNegatedCondition(Factory f, CtAssert<?> assertion) {
        CtUnaryOperator<Boolean> negCondition = f.Core().createUnaryOperator();
        negCondition.setKind(UnaryOperatorKind.NOT);
        negCondition.setOperand(assertion.getAssertExpression());
        return negCondition;
    }

    private CtInvocation<?> createGetValueInvocation(Factory f, CtLocalVariable<Maybe> auxVar, CtTypeReference<?> returnCallType) {
        CtInvocation<?> valueInvoc =
                f.Code().createInvocation(
                        f.Code().createVariableRead(auxVar.getReference(), false),
                        f.Executable().createReference(
                                f.Type().createReference(Maybe.class),
                                returnCallType,
                                "getValue")
                );
        return valueInvoc;
    }

    private CtLocalVariable<Maybe> createAuxiliaryVariableDeclaration(Factory f, String auxVarName, CtTypeReference<?> returnCallType) {
        CtLocalVariable<Maybe> auxVar = f.Core().createLocalVariable();
        auxVar.setSimpleName(auxVarName);
        CtTypeReference<Maybe> maybeRef = f.Type().createReference(Maybe.class);
        maybeRef.addActualTypeArgument(getHeapType(returnCallType));
        auxVar.setType(maybeRef);
        return auxVar;
    }

    private CtAbstractInvocation<?> createModifiedInvocation(CtAbstractInvocation<?> call, Factory f) {
        String modifiedName;
        CtAbstractInvocation<?> invocation;
        if (call instanceof CtInvocation) {
            modifiedName = call.getExecutable().getSimpleName() + "Copy";
            
            invocation = f.Code().createInvocation(
                    ((CtInvocation<?>)call).getTarget(),
                    f.Executable().createReference(
                            call.getExecutable().getDeclaringType(),
                            call.getExecutable().getType(),
                            modifiedName,
                            call.getExecutable().getParameters()),
                    call.getArguments());
        } else {
            modifiedName =
                    call.getExecutable().getType().getSimpleName() + "Factory";
            invocation = f.Code().createInvocation(
                    null,
                    f.Executable().createReference(
                            call.getExecutable().getDeclaringType(),
                            true,
                            call.getExecutable().getDeclaringType(),
                            modifiedName,
                            call.getExecutable().getParameters()),
                    call.getArguments()
            );
        }
        return invocation;
    }

    private CtIf createGenerationConditional(Factory f, CtAssert<?> assertion, 
            CtTypeReference<Maybe> maybe, String methodName, int index) {
        CtIf conditional = f.Core().createIf();
        CtUnaryOperator<Boolean> assertConditionNeg = f.Core().createUnaryOperator();
        assertConditionNeg.setKind(UnaryOperatorKind.NOT);
        assertConditionNeg.setOperand(assertion.getAssertExpression());
        conditional.setCondition(assertConditionNeg);
        CtInvocation<Maybe> generateErrorCall =
                f.Code().createInvocation(
                        null,
                        f.Executable().createReference(
                                maybe,
                                true,
                                maybe,
                                "generateError",
                                asList(
                                        f.Type().STRING,
                                        f.Type().INTEGER_PRIMITIVE,
                                        f.Type().OBJECT
                                )
                        ),
                        f.Code().createLiteral(methodName),
                        f.Code().createLiteral(index),
                        (assertion.getExpression() == null ?
                                f.Code().createLiteral(null) :
                                assertion.getExpression())
                );
        CtReturn<Maybe> thenBranch = f.Core().createReturn();
        thenBranch.setReturnedExpression(generateErrorCall);
        conditional.setThenStatement(thenBranch);
        return conditional;
    }
    
    private List<InsertionPoint> getInsertionPoints(CtStatement call) {
        CtStatement current = call;
        CtStatement parent = current.getParent(CtStatement.class);
        while (parent instanceof CtExpression) {
            current = parent;
            parent = current.getParent(CtStatement.class);
        }
        
        if (parent instanceof CtStatementList ||
                parent instanceof CtSynchronized ||
                parent instanceof CtTry) { // TODO: Try-with-resources
            return singletonList(new InsertionPoint(InsertDirection.BEFORE, current));
        } else if (parent instanceof CtThrow ||
                parent instanceof CtReturn ||
                parent instanceof CtAssert ||
                parent instanceof CtSwitch ||
                parent instanceof CtAssignment ||
                parent instanceof CtLocalVariable) {
            return singletonList(new InsertionPoint(InsertDirection.BEFORE, parent));
        } else if (parent instanceof CtWhile) {
            CtWhile parentWhile = (CtWhile) parent;
            if (parentWhile.getLoopingExpression().getAnnotatedChildren(CallTransform.class).contains(call)) {
                return asList(
                        new InsertionPoint(InsertDirection.BEFORE, parentWhile),
                        new InsertionPoint(InsertDirection.AFTER, getLastSubstatement(parentWhile.getBody()))
                );
            } else {
                return singletonList(new InsertionPoint(InsertDirection.BEFORE, current));
            }
        } else if (parent instanceof CtDo) {
            CtDo parentDo = (CtDo) parent;
            if (parentDo.getLoopingExpression().getAnnotatedChildren(CallTransform.class).contains(call)) {
                return singletonList(new InsertionPoint(InsertDirection.AFTER, getLastSubstatement(parentDo.getBody())));
            } else {
                return singletonList(new InsertionPoint(InsertDirection.BEFORE, current));
            }
        } else if (parent instanceof CtFor) {
            // TODO: What if the call belongs to the initialization or increment section?
            CtFor parentFor = (CtFor) parent;
            if (parentFor.getExpression().getAnnotatedChildren(CallTransform.class).contains(call)) {
                return asList(
                        new InsertionPoint(InsertDirection.BEFORE, parentFor),
                        new InsertionPoint(InsertDirection.AFTER, getLastSubstatement(parentFor.getBody()))
                );
            } else {
                return singletonList(new InsertionPoint(InsertDirection.BEFORE, current));
            }
        } else if (parent instanceof CtForEach) {
            CtForEach parentForEach = (CtForEach) parent;
            if (parentForEach.getExpression().getAnnotatedChildren(CallTransform.class).contains(call)) {
                return asList(
                        new InsertionPoint(InsertDirection.BEFORE, parentForEach),
                        new InsertionPoint(InsertDirection.AFTER, getLastSubstatement(parentForEach.getBody()))
                );
            } else {
                return singletonList(new InsertionPoint(InsertDirection.BEFORE, current));
            }
        } else if (parent instanceof CtIf) {
            CtIf parentIf = (CtIf) parent;
            if (parentIf.getCondition().getAnnotatedChildren(CallTransform.class).contains(call)) {
                return singletonList(new InsertionPoint(InsertDirection.BEFORE, parentIf));
            } else {
                return singletonList(new InsertionPoint(InsertDirection.BEFORE, current));
            }
        } else {
            return Collections.EMPTY_LIST;
        }
    }
    
    private CtStatement getLastSubstatement(CtStatement stm) {
        if (stm instanceof CtBlock) {
            return ((CtBlock)stm).getLastStatement();
        } else {
            return stm;
        }
    }

}
