package org.hibernate.search.engine.search.predicate.spi;

import org.hibernate.search.engine.search.common.ValueModel;

public interface MatchIdPredicateBuilder extends SearchPredicateBuilder {

	void value(Object value, ValueModel valueModel);

}
