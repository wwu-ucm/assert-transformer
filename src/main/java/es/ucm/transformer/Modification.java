package es.ucm.transformer;

abstract public class Modification {

	protected int position;

	public Modification(int position) {
		this.position = position;
	}

	public int getPosition() {
		return position;
	}

}