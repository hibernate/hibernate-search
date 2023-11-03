/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.binding.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeOptionsStep;
import org.hibernate.search.engine.backend.types.dsl.ScaledNumberIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.backend.types.dsl.StringIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.backend.types.dsl.VectorFieldTypeOptionsStep;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.spi.FieldModelContributorContext;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

final class FieldModelContributorContextImpl<F> implements FieldModelContributorContext {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final ValueBridge<?, F> bridge;
	private final IndexFieldTypeOptionsStep<?, ? super F> fieldTypeOptionsStep;

	FieldModelContributorContextImpl(ValueBridge<?, F> bridge, IndexFieldTypeOptionsStep<?, ? super F> fieldTypeOptionsStep) {
		this.bridge = bridge;
		this.fieldTypeOptionsStep = fieldTypeOptionsStep;
	}

	@Override
	public void indexNullAs(String value) {
		standardTypeOptionsStep().indexNullAs( bridge.parse( value ) );
	}

	/*
	 * If fieldTypeOptionsStep is an instance of IndexFieldTypeOptionsStep<?, ? super F>
	 * and StandardIndexFieldTypeOptionsStep,
	 * it's an instance of StandardIndexFieldTypeOptionsStep<?, ? super F>
	 */
	@SuppressWarnings("unchecked")
	@Override
	public StandardIndexFieldTypeOptionsStep<?, ? super F> standardTypeOptionsStep() {
		if ( fieldTypeOptionsStep instanceof StandardIndexFieldTypeOptionsStep ) {
			return (StandardIndexFieldTypeOptionsStep<?, ? super F>) fieldTypeOptionsStep;
		}
		else {
			throw log.invalidFieldEncodingForStandardFieldMapping(
					fieldTypeOptionsStep, StandardIndexFieldTypeOptionsStep.class
			);
		}
	}

	@Override
	public StringIndexFieldTypeOptionsStep<?> stringTypeOptionsStep() {
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
	public ScaledNumberIndexFieldTypeOptionsStep<?, ?> scaledNumberTypeOptionsStep() {
		if ( fieldTypeOptionsStep instanceof ScaledNumberIndexFieldTypeOptionsStep ) {
			return (ScaledNumberIndexFieldTypeOptionsStep<?, ?>) fieldTypeOptionsStep;
		}
		else {
			throw log.invalidFieldEncodingForScaledNumberFieldMapping(
					fieldTypeOptionsStep, ScaledNumberIndexFieldTypeOptionsStep.class
			);
		}
	}

	@Override
	public VectorFieldTypeOptionsStep<?, ?> vectorTypeOptionsStep() {
		if ( fieldTypeOptionsStep instanceof VectorFieldTypeOptionsStep ) {
			return (VectorFieldTypeOptionsStep<?, ?>) fieldTypeOptionsStep;
		}
		else {
			throw log.invalidFieldEncodingForVectorFieldMapping(
					fieldTypeOptionsStep, VectorFieldTypeOptionsStep.class
			);
		}
	}

	@Override
	public void checkNonStandardTypeOptionsStep() {
		if ( fieldTypeOptionsStep instanceof StandardIndexFieldTypeOptionsStep ) {
			throw log.invalidFieldEncodingForNonStandardFieldMapping(
					fieldTypeOptionsStep, StandardIndexFieldTypeOptionsStep.class
			);
		}
	}
}
