/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.types.dsl.impl;

import java.math.BigDecimal;

import org.hibernate.search.engine.backend.types.dsl.ScaledNumberIndexFieldTypeContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexFieldTypeDefaultsProvider;

public class StubScaledNumberIndexFieldTypeContext
		extends AbstractStubStandardIndexFieldTypeContext<StubScaledNumberIndexFieldTypeContext, BigDecimal>
		implements ScaledNumberIndexFieldTypeContext<StubScaledNumberIndexFieldTypeContext, BigDecimal> {

	public StubScaledNumberIndexFieldTypeContext(IndexFieldTypeDefaultsProvider defaultsProvider) {
		super( BigDecimal.class );
		setDefaults( defaultsProvider );
	}

	@Override
	StubScaledNumberIndexFieldTypeContext thisAsS() {
		return this;
	}

	@Override
	public StubScaledNumberIndexFieldTypeContext decimalScale(int decimalScale) {
		modifiers.add( b -> b.decimalScale( decimalScale ) );
		return this;
	}

	private void setDefaults(IndexFieldTypeDefaultsProvider defaultsProvider) {
		Integer decimalScale = defaultsProvider.getDecimalScale();
		if ( decimalScale != null ) {
			modifiers.add( b -> b.defaultDecimalScale( decimalScale ) );
		}
	}
}
