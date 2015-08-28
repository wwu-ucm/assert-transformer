package es.ucm.asserttransformer.processors;

import es.ucm.asserttransformer.annotations.AssertStmTransform;
import es.ucm.asserttransformer.annotations.CallTransform;
import es.ucm.asserttransformer.annotations.MethodTransform;
import java.lang.annotation.Annotation;
import spoon.processing.AbstractAnnotationProcessor;
import spoon.reflect.code.CtAbstractInvocation;
import spoon.reflect.code.CtAssert;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtMethod;

public class RemoveAuxiliaryAnnotations extends AbstractAnnotationProcessor<Annotation, CtElement> {

    @Override
    public void init() {
        super.init();
        this.clearConsumedAnnotationTypes();
        this.addConsumedAnnotationType(AssertStmTransform.class);
        this.addConsumedAnnotationType(CallTransform.class);
        this.addConsumedAnnotationType(MethodTransform.class);
        this.clearProcessedAnnotationTypes();
        this.addProcessedAnnotationType(AssertStmTransform.class);
        this.addProcessedAnnotationType(CallTransform.class);
        this.addProcessedAnnotationType(MethodTransform.class);
        this.clearProcessedElementType();
        this.addProcessedElementType(CtMethod.class);
        this.addProcessedElementType(CtConstructor.class);
        this.addProcessedElementType(CtAssert.class);
        this.addProcessedElementType(CtAbstractInvocation.class);
    }

    @Override
    public void process(Annotation a, CtElement e) {
    }
}
