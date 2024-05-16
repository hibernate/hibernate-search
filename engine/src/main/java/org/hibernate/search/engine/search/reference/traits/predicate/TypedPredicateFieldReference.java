package org.hibernate.search.engine.search.reference.traits.predicate;

import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.reference.traits.FieldReference;

public interface TypedPredicateFieldReference<C, T> extends FieldReference<C> {

	Class<T> predicateType();

	default ValueConvert valueConvert() {
		return ValueConvert.YES;
	}
}
