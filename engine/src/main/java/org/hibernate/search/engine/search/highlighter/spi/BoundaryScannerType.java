package org.hibernate.search.engine.search.highlighter.spi;

public enum BoundaryScannerType {
	/**
	 * Default is dependent on a {@link SearchHighlighterType highlighter type} being used.
	 */
	DEFAULT,
	CHARS, SENTENCE, WORD;
}
