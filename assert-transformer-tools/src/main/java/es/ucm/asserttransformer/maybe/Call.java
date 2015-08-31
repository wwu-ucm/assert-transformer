package es.ucm.asserttransformer.maybe;

public class Call {

    private int callNumber;
    private String callName;
    private Object expression;

    public Call(String callName, int callNumber, Object expression) {
        this.callNumber = callNumber;
        this.callName = callName;
        this.expression = expression;
    }

    public int getCallNumber() {
        return callNumber;
    }

    public String getCallName() {
        return callName;
    }

    @Override
    public String toString() {
        return callName + "." + callNumber + ": "
                + (expression != null ? expression.toString() : "");
    }
}
