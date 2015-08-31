package es.ucm.asserttransformer.maybe;

// Methods returning T values now
// return Option<T> values
public abstract class Maybe<T> {

    public static class Value<T> extends Maybe<T> {

        T value;

        public Value(T value) {
            this.value = value;
        }

        @Override
        public boolean isValue() {
            return true;
        }

        @Override
        public T getValue() {
            return value;
        }

        @Override
        public String toString() {
            return value.toString();
        }
    }

    public static class CondError<T> extends Maybe<T> {

        private PureList<Call> callStack;
		//private List<Call> callStack;

        public CondError() {
            this.callStack = PureList.empty();
            //this.callStack = new ArrayList<Call>();
        }

        public CondError(Call newElement) {
            PureList<Call> empty = PureList.empty();
            this.callStack = PureList.createNode(newElement, empty);
			//this.callStack = new ArrayList<Call>();
            //this.callStack.add(newElement);
        }

        public <S> CondError(Call newElement, CondError<S> other) {
            this.callStack = PureList.createNode(newElement, other.callStack);
			//this.callStack = new ArrayList<Call>(other.callStack);
            //this.callStack.add(newElement);
        }

        public PureList<Call> getCallStack() {
            //public List<Call> getCallStack() {
            return callStack;
        }

        @Override
        public boolean isValue() {
            return false;
        }

        @Override
        public T getValue() {
            return null;
        }

        @Override
        public String toString() {
            return (callStack.toString());
        }

        /*		public List<Call> getList() {
         return callStack;
         }*/
    }

	// has the method returned a normal value? 
    // i.e. no condition violation has been detected?
    abstract public boolean isValue();

    // the value returned by the method.
    abstract public T getValue();

	// The condition violation has been detected.
    // return the same value as before instrumentation 
    public static <K> Maybe<K> createValue(K value) {
        return new Value<K>(value);
    }

    // an assert condition is not verified
    public static <T> Maybe<T> generateError(String method, int position, Object exp) {
        return new CondError<T>(new Call(method, position, exp));
    }

    // this method calls another method whose precondition or postcondition is not verified
    public static <T, S> Maybe<T> propagateError(String method, int position,
            Maybe<S> other) {
        return new CondError<T>(new Call(method, position, null), (CondError<S>) other);
    }

}
