package org.hibernate.search.engine.search.predicate.spi;

import static org.hibernate.search.engine.search.common.spi.SearchQueryElementTypeKey.of;

import org.hibernate.search.engine.backend.types.IndexFieldTraits;
import org.hibernate.search.engine.search.common.spi.SearchQueryElementTypeKey;

public final class ScoreFunctionTypeKeys {

	private ScoreFunctionTypeKeys() {
	}

	public static final SearchQueryElementTypeKey<FieldValueFactorFunctionBuilder> FIELD_VALUE_FACTOR =
			of( IndexFieldTraits.ScoreFunctions.FIELD_VALUE_FACTOR );

}
