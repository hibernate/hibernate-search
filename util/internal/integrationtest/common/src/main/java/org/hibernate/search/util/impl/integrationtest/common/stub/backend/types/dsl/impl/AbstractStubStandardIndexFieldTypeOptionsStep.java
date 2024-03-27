/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.types.dsl.impl;

import org.hibernate.search.engine.backend.types.Aggregable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeOptionsStep;

abstract class AbstractStubStandardIndexFieldTypeOptionsStep<S extends AbstractStubStandardIndexFieldTypeOptionsStep<?, F>, F>
		extends AbstractStubSearchableProjectableIndexFieldTypeOptionsStep<S, F>
		implements StandardIndexFieldTypeOptionsStep<S, F> {

	AbstractStubStandardIndexFieldTypeOptionsStep(Class<F> inputType) {
		super( inputType );
	}

	@Override
	public S sortable(Sortable sortable) {
		builder.modifier( b -> b.sortable( sortable ) );
		return thisAsS();
	}

	@Override
	public S aggregable(Aggregable aggregable) {
		builder.modifier( b -> b.aggregable( aggregable ) );
		return thisAsS();
	}

}
