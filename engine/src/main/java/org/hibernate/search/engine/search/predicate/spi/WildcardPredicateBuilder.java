package org.hibernate.search.engine.search.predicate.spi;

public interface WildcardPredicateBuilder extends SearchPredicateBuilder {

	void pattern(String wildcardPattern);

}
