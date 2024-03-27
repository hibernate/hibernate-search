/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.types.dsl.impl;

import org.hibernate.search.engine.backend.types.dsl.ScaledNumberIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexFieldTypeDefaultsProvider;

public class StubScaledNumberIndexFieldTypeOptionsStep<F extends Number>
		extends AbstractStubStandardIndexFieldTypeOptionsStep<StubScaledNumberIndexFieldTypeOptionsStep<F>, F>
		implements ScaledNumberIndexFieldTypeOptionsStep<StubScaledNumberIndexFieldTypeOptionsStep<F>, F> {

	public StubScaledNumberIndexFieldTypeOptionsStep(Class<F> fieldType, IndexFieldTypeDefaultsProvider defaultsProvider) {
		super( fieldType );
		setDefaults( defaultsProvider );
	}

	@Override
	StubScaledNumberIndexFieldTypeOptionsStep<F> thisAsS() {
		return this;
	}

	@Override
	public StubScaledNumberIndexFieldTypeOptionsStep<F> decimalScale(int decimalScale) {
		builder.modifier( b -> b.decimalScale( decimalScale ) );
		return this;
	}

	private void setDefaults(IndexFieldTypeDefaultsProvider defaultsProvider) {
		Integer decimalScale = defaultsProvider.decimalScale();
		if ( decimalScale != null ) {
			builder.modifier( b -> b.defaultDecimalScale( decimalScale ) );
		}
	}
}
