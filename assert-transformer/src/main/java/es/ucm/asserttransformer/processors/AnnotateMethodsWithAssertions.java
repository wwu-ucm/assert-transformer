/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.ucm.asserttransformer.processors;

import es.ucm.asserttransformer.annotations.AssertTransform;
import spoon.processing.AbstractProcessor;
import spoon.reflect.code.CtAssert;
import spoon.reflect.declaration.CtExecutable;

/**
 *
 * @author manuel
 */
public class AnnotateMethodsWithAssertions extends AbstractProcessor<CtAssert> {

    @Override
    public void process(CtAssert e) {
        CtExecutable<?> containerMethod = e.getParent(CtExecutable.class);
        getFactory().Annotation().annotate(containerMethod, AssertTransform.class);
    }

}
