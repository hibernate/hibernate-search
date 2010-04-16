package org.hibernate.search.query.dsl.v2.impl;

/**
* @author Emmanuel Bernard
*/
public class QueryContext {
	private final Approximation approximation;
	private float threshold = .5f;
	private int prefixLength = 0;

	public QueryContext(Approximation approximation) {
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
