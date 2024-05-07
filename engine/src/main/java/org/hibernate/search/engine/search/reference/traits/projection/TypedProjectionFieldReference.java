package org.hibernate.search.engine.search.reference.traits.projection;

import org.hibernate.search.engine.search.common.ValueConvert;

public interface TypedProjectionFieldReference<C, T> extends ProjectionFieldReference<C> {

	Class<T> projectionType();

	default ValueConvert valueConvert() {
		return ValueConvert.YES;
	}
}
