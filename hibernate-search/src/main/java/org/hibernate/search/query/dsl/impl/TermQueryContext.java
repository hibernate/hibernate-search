package org.hibernate.search.query.dsl.impl;

/**
* @author Emmanuel Bernard
*/
class TermQueryContext {
	private final Approximation approximation;
	//FUZZY
	private float threshold = .5f;

	//WILDCARD
	private int prefixLength = 0;

	public TermQueryContext(Approximation approximation) {
		this.approximation = approximation;
	}

	public void setThreshold(float threshold) {
		this.threshold = threshold;
	}

	public void setPrefixLength(int prefixLength) {
		this.prefixLength = prefixLength;
	}

	public Approximation getApproximation() {
		return approximation;
	}

	public float getThreshold() {
		return threshold;
	}

	public int getPrefixLength() {
		return prefixLength;
	}

	public static enum Approximation {
		EXACT,
		WILDCARD,
		FUZZY
	}
}
