package org.hibernate.search.engine.search.predicate.spi;

public interface MinimumShouldMatchBuilder {
	void minimumShouldMatchNumber(int ignoreConstraintCeiling, int matchingClausesNumber);

	void minimumShouldMatchPercent(int ignoreConstraintCeiling, int matchingClausesPercent);

}
