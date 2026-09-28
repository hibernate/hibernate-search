package org.hibernate.search.engine.search.predicate.spi;

public interface PhrasePredicateBuilder extends SearchPredicateBuilder {

	void slop(int slop);

	void phrase(String phrase);

	void analyzer(String analyzerName);

	void skipAnalysis();
}
