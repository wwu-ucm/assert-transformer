package es.ucm.asserttransformer.processors;

import es.ucm.asserttransformer.Globals;
import es.ucm.asserttransformer.annotations.CallTransform;
import es.ucm.asserttransformer.annotations.MethodTransform;
import spoon.processing.AbstractProcessor;
import spoon.reflect.code.CtAbstractInvocation;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtExecutable;

public class AnnotateMethodsLk extends AbstractProcessor<CtAbstractInvocation<?>> {

    @Override
    public void process(CtAbstractInvocation<?> e) {
        CtExecutable<?> calledMethod = e.getExecutable().getDeclaration();
        
        // If source code of the method being called is available
        if (calledMethod != null) {
            MethodTransform annCalled = calledMethod.getAnnotation(MethodTransform.class);
            CtExecutable<?> parent = e.getParent(CtExecutable.class);
            MethodTransform annParent = parent.getAnnotation(MethodTransform.class);
            if (annCalled != null && (annCalled.level() < Globals.maxLevel || annParent != null)) {
                getFactory().Annotation().annotate(e, CallTransform.class);
                if (annParent == null) {
                    int nextLevel = annCalled.level() + 1;
                    Globals.numTaggedMethods = Globals.numTaggedMethods + 1;
                    System.out.println("Found: " + e.getSignature() + " [level = " + nextLevel + "]");
                    CtAnnotation<MethodTransform> newAnn =
                            getFactory().Annotation().annotate(parent, MethodTransform.class);
                    newAnn.addValue("level", nextLevel);
                }
            }
        }
    }
    
}
