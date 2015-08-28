package es.ucm.asserttransformer.maybe;

abstract public class PureList<T> {
	
	public static class EmptyNode<T> extends PureList<T> {
		@Override
		public boolean isEmpty() {
			return true;
		}
		@Override
		public String toString(){
			return "";
		}
		@Override
		public T getHead() {
			return null;
		}
		@Override
		public PureList<T> getTail() {
			return null;
		}
	}
	
	public static class ConsNode<T> extends PureList<T> {
		private T element;
		private PureList<T> next;

		public ConsNode(T element, PureList<T> next) {
			this.element = element;
			this.next = next;
		}

		public T getElement() {
			return element;
		}

		public PureList<T> getNext() {
			return next;
		}

		@Override
		public boolean isEmpty() {
			return false;
		}
		
		@Override
		public String toString(){
			return element + " | " + next;
		}

		@Override
		public T getHead() {
			return element;
		}

		@Override
		public PureList<T> getTail() {
			return next;
		}

	}
	
	abstract public boolean isEmpty();
	abstract public T getHead();
	abstract public PureList<T> getTail();
	
	public static <K> PureList<K> empty() {
		return new EmptyNode<K>();
	}
	
	public static <K> PureList<K> createNode(K element, PureList<K> next) {
		return new ConsNode<K>(element, next);
	}
	
	
}
