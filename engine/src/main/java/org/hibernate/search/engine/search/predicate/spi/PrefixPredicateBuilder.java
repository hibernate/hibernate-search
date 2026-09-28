package org.hibernate.search.engine.search.predicate.spi;

public interface PrefixPredicateBuilder extends SearchPredicateBuilder {

	void prefix(String prefix);

}
