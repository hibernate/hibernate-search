/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.binding.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.engine.backend.types.dsl.ScaledNumberIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.backend.types.dsl.StringIndexFieldTypeOptionsStep;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.spi.FieldModelContributorContext;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

final class FieldModelContributorContextImpl<F> implements FieldModelContributorContext {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final ValueBridge<?, F> bridge;
	private final StandardIndexFieldTypeOptionsStep<?, ? super F> fieldTypeOptionsStep;

	FieldModelContributorContextImpl(ValueBridge<?, F> bridge, StandardIndexFieldTypeOptionsStep<?, ? super F> fieldTypeOptionsStep) {
		this.bridge = bridge;
		this.fieldTypeOptionsStep = fieldTypeOptionsStep;
	}

	@Override
	public void indexNullAs(String value) {
		fieldTypeOptionsStep.indexNullAs( bridge.parse( value ) );
	}

	@Override
	public StandardIndexFieldTypeOptionsStep<?, ? super F> getStandardTypeOptionsStep() {
		return fieldTypeOptionsStep;
	}

	@Override
	public StringIndexFieldTypeOptionsStep<?> getStringTypeOptionsStep() {
		if ( fieldTypeOptionsStep instanceof StringIndexFieldTypeOptionsStep ) {
			return (StringIndexFieldTypeOptionsStep<?>) fieldTypeOptionsStep;
		}
		else {
			throw log.invalidFieldEncodingForStringFieldMapping(
					fieldTypeOptionsStep, StringIndexFieldTypeOptionsStep.class
			);
		}
	}

	@Override
	public ScaledNumberIndexFieldTypeOptionsStep<?, ?> getScaledNumberTypeOptionsStep() {
		if ( fieldTypeOptionsStep instanceof ScaledNumberIndexFieldTypeOptionsStep ) {
			return (ScaledNumberIndexFieldTypeOptionsStep<?, ?>) fieldTypeOptionsStep;
		}
		else {
			throw log.invalidFieldEncodingForScaledNumberFieldMapping(
					fieldTypeOptionsStep, ScaledNumberIndexFieldTypeOptionsStep.class
			);
		}
	}
}
