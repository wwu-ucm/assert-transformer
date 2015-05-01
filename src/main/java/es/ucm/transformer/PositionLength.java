package es.ucm.transformer;

public class PositionLength {
	private int position;
	private int length;
	
	public PositionLength(int position, int length) {
		this.position = position;
		this.length = length;
	}

	public int getPosition() {
		return position;
	}
	
	public int getLength() {
		return length;
	}

	@Override
	public String toString() {
		return "PositionLength [position=" + position + ", length=" + length
				+ "]";
	}
}
