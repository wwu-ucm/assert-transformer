/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.ucm.asserttransformer.maybe;

/**
 *
 * @author manuel
 */
public class ResultContainer<T> {

    public Maybe<T> value;

    public ResultContainer() {
        value = null;
    }

    public void setValue(Maybe<T> value) {
        this.value = value;
    }

    public Maybe<T> orElse(T other) {
        if (value == null) {
            return Maybe.createValue(other);
        } else {
            return value;
        }
    }
}
