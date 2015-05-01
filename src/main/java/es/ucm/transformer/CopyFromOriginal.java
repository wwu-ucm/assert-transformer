package es.ucm.transformer;

public class CopyFromOriginal extends Modification {
	private int length;
	private int originalPosition;

	public CopyFromOriginal(int position, int originalPosition, int length) {
		super(position);
		this.originalPosition = originalPosition;
		this.length = length;
	}
	
	public int getLength() {
		return length;
	}		 
	
	public int getOriginalPosition() {
		return originalPosition;
	}
}
