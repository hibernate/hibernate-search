/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.building.impl;

import org.hibernate.search.engine.backend.types.dsl.ScaledNumberIndexFieldTypeContext;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeContext;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.mapping.building.spi.FieldModelContributorIndirectContext;

/**
 * Provides an <b>indirect</b> way to contribute on {@link StandardIndexFieldTypeContext}.
 *
 * Useful for the case when the contribution is suitable of translation.
 * For instance, if the value must be converted using a {@link ValueBridge}.
 *
 * Moreover, it could be useful whether the applying of the contribution
 * could depends on other contributions applied previously
 * on the same {@link StandardIndexFieldTypeContext}.
 * For instance, a default value should not be applied
 * if the same value has been provided explicitly.
 *
 * @param <F> The type of raw index field values, on the index side of the bridge.
 */
public class FieldModelContributorIndirectContextImpl<F> implements FieldModelContributorIndirectContext {

	private final ValueBridge<?, F> bridge;
	private final StandardIndexFieldTypeContext<?, ? super F> fieldTypeContext;

	private boolean useDefaultDecimalScale = true;

	public FieldModelContributorIndirectContextImpl(ValueBridge<?, F> bridge, StandardIndexFieldTypeContext<?, ? super F> fieldTypeContext) {
		this.bridge = bridge;
		this.fieldTypeContext = fieldTypeContext;
	}

	@Override
	public void indexNullAs(String value) {
		fieldTypeContext.indexNullAs( bridge.parse( value ) );
	}

	@Override
	public void decimalScale(int decimalScale) {
		if ( fieldTypeContext instanceof ScaledNumberIndexFieldTypeContext ) {
			( (ScaledNumberIndexFieldTypeContext) fieldTypeContext ).decimalScale( decimalScale );
			useDefaultDecimalScale = false;
		}
	}

	@Override
	public void defaultDecimalScale(int decimalScale) {
		if ( useDefaultDecimalScale && fieldTypeContext instanceof ScaledNumberIndexFieldTypeContext ) {
			( (ScaledNumberIndexFieldTypeContext) fieldTypeContext ).decimalScale( decimalScale );
		}
	}
}
