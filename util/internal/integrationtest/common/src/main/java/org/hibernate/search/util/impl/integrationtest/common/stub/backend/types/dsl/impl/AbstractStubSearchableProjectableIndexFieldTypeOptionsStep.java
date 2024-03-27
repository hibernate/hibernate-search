/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.types.dsl.impl;

import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.engine.backend.types.dsl.SearchableProjectableIndexFieldTypeOptionsStep;

abstract class AbstractStubSearchableProjectableIndexFieldTypeOptionsStep<
		S extends AbstractStubSearchableProjectableIndexFieldTypeOptionsStep<?, F>,
		F>
		extends AbstractStubIndexFieldTypeOptionsStep<S, F>
		implements SearchableProjectableIndexFieldTypeOptionsStep<S, F> {

	AbstractStubSearchableProjectableIndexFieldTypeOptionsStep(Class<F> inputType) {
		super( inputType );
	}

	@Override
	public S searchable(Searchable searchable) {
		builder.modifier( b -> b.searchable( searchable ) );
		return thisAsS();
	}

	@Override
	public S projectable(Projectable projectable) {
		builder.modifier( b -> b.projectable( projectable ) );
		return thisAsS();
	}

	@Override
	public S indexNullAs(F indexNullAs) {
		builder.modifier( b -> b.indexNullAs( indexNullAs ) );
		return thisAsS();
	}
}
