package es.ucm.transformer;

public class Insertion extends Modification {
	private String text;

	public Insertion(int position, String text) {
		super(position);
		this.text = text;
	}
	
	public String getText() {
		return text;
	}

	@Override
	public String toString() {
		return "Insertion [" + position + ", " + text + "]";
	}
}
