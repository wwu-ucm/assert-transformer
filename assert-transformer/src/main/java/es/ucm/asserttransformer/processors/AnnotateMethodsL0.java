package es.ucm.asserttransformer.processors;

import es.ucm.asserttransformer.Globals;
import es.ucm.asserttransformer.annotations.AssertStmTransform;
import es.ucm.asserttransformer.annotations.AssertTransform;
import es.ucm.asserttransformer.annotations.MethodTransform;
import java.lang.annotation.Annotation;
import java.util.List;
import spoon.processing.AbstractAnnotationProcessor;
import spoon.reflect.code.CtAssert;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtGenericElement;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtNamedElement;

public class AnnotateMethodsL0 extends AbstractAnnotationProcessor<AssertTransform, CtNamedElement> {

    @Override
    public void init() {
        super.init();
        addProcessedElementType(CtMethod.class);
        addProcessedElementType(CtConstructor.class);
    }

    
    
    @Override
    public void process(AssertTransform a, CtNamedElement e) {
        getFactory().Annotation().annotate(e, MethodTransform.class);
        Globals.numTaggedMethods = Globals.numTaggedMethods + 1;
        System.out.println("Found: " + e.getSignature() + " [level = 0]");
        List<CtAssert<?>> assertions = e.getElements(asrt -> true);
        assertions.forEach(
                asrt -> getFactory().Annotation().annotate(asrt, AssertStmTransform.class)
        );
    }

    @Override
    public boolean shoudBeConsumed(CtAnnotation<? extends Annotation> annotation) {
        return false;
    }
}
