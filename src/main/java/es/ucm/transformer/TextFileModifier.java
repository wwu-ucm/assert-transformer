package es.ucm.transformer;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class TextFileModifier {
	public static void modify(List<Modification> modifications, Reader input, Writer output, String original) throws IOException {
		int charCounter = 0;
		sortInsertions(modifications);
		
		Iterator<Modification> insertionsIt = modifications.iterator();
		Modification nextModification = advanceIterator(insertionsIt);
		
		boolean hasMoreCharacters = true;
		do {
			if (nextModification != null && nextModification.getPosition() <= charCounter) {
				if (nextModification instanceof Insertion) {
					Insertion nextInsertion = (Insertion) nextModification;
					output.write(nextInsertion.getText());
				} else if (nextModification instanceof Removal) {
					Removal removal = (Removal) nextModification;
					for (int i = 0; i < removal.getNumChars(); i++) {
						int charRead = input.read();
						charCounter++;
						if (charRead == -1) hasMoreCharacters = false;
					}
				} else if (nextModification instanceof CopyFromOriginal) {
					CopyFromOriginal copy = (CopyFromOriginal) nextModification;
					output.write(original, copy.getOriginalPosition(), copy.getLength());
				}
				nextModification = advanceIterator(insertionsIt);
			} else {
				int charRead = input.read();
				if (charRead != -1) {
					output.write(charRead);
					charCounter++;
				} else {
					hasMoreCharacters = false;
				}
			}
		} while (nextModification != null || hasMoreCharacters);
	}

	private static void sortInsertions(List<Modification> insertions) {
		Collections.sort(insertions, new Comparator<Modification>() {
			@Override
			public int compare(Modification arg0, Modification arg1) {
				return Integer.compare(arg0.getPosition(), arg1.getPosition());
			}
		});
	}

	private static Modification advanceIterator(Iterator<Modification> insertionsIt) {
		if (insertionsIt.hasNext()) {
			return insertionsIt.next();
		} else {
			return null;
		}
	}

}
