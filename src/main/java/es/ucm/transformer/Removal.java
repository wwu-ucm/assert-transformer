package es.ucm.transformer;

public class Removal extends Modification {
	private int numChars;

	public Removal(int position, int numChars) {
		super(position);
		this.numChars = numChars;
	}
	
	public int getNumChars() {
		return numChars;
	}
}
