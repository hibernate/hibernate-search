/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.model.singlefield;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.search.engine.backend.common.spi.FieldPaths;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.backend.types.dsl.SearchableProjectableIndexFieldTypeOptionsStep;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.IndexFieldLocation;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.IndexObjectFieldCardinality;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TestedFieldStructure;

/**
 * An index binding for tests focusing on a single field at a time.
 */
public class SingleFieldIndexBinding extends AbstractObjectBinding {
	public static final String DISCRIMINATOR_VALUE_INCLUDED = "included";
	public static final String DISCRIMINATOR_VALUE_EXCLUDED = "excluded";

	public static final Consumer<SearchableProjectableIndexFieldTypeOptionsStep<?, ?>> NO_ADDITIONAL_CONFIGURATION = c -> {};

	public static <S extends SearchableProjectableIndexFieldTypeOptionsStep<?, ?>> SingleFieldIndexBinding create(
			IndexSchemaElement root,
			Collection<? extends FieldTypeDescriptor<?, ? extends S>> supportedFieldTypes,
			Consumer<S> additionalConfiguration) {
		return new SingleFieldIndexBinding( root, supportedFieldTypes, additionalConfiguration,
				IndexObjectFieldCardinality.MULTI_VALUED );
	}

	public static <
			S extends SearchableProjectableIndexFieldTypeOptionsStep<?,
					?>> SingleFieldIndexBinding createWithSingleValuedNestedFields(
							IndexSchemaElement root,
							Collection<? extends FieldTypeDescriptor<?, ? extends S>> supportedFieldTypes,
							Consumer<S> additionalConfiguration) {
		return new SingleFieldIndexBinding( root, supportedFieldTypes, additionalConfiguration,
				IndexObjectFieldCardinality.SINGLE_VALUED );
	}

	public final FirstLevelObjectBinding flattenedObject;
	public final FirstLevelObjectBinding nestedObject;

	private <S extends SearchableProjectableIndexFieldTypeOptionsStep<?, ?>> SingleFieldIndexBinding(
			IndexSchemaElement root, Collection<? extends FieldTypeDescriptor<?, ? extends S>> supportedFieldTypes,
			Consumer<? super S> additionalConfiguration,
			IndexObjectFieldCardinality nestedFieldCardinality) {
		super( null, null, root, supportedFieldTypes, additionalConfiguration );
		flattenedObject = FirstLevelObjectBinding.create(
				this, "flattenedObject", root, ObjectStructure.FLATTENED,
				supportedFieldTypes, additionalConfiguration, nestedFieldCardinality
		);
		nestedObject = FirstLevelObjectBinding.create(
				this, "nestedObject", root, ObjectStructure.NESTED,
				supportedFieldTypes, additionalConfiguration, nestedFieldCardinality
		);
	}

	public final String getFieldPath(TestedFieldStructure fieldStructure, FieldTypeDescriptor<?, ?> fieldType) {
		return getParentObject( fieldStructure ).getAbsoluteFieldPath( fieldStructure, fieldType );
	}

	public final String getDiscriminatorFieldPath(TestedFieldStructure fieldStructure) {
		return FieldPaths.compose( getParentObject( fieldStructure ).absolutePath, DISCRIMINATOR_FIELD_NAME );
	}

	public final AbstractObjectBinding getParentObject(TestedFieldStructure fieldStructure) {
		switch ( fieldStructure.location ) {
			case ROOT:
				return this;
			case IN_FLATTENED:
				return flattenedObject;
			case IN_NESTED:
				return nestedObject;
			case IN_NESTED_TWICE:
				return nestedObject.nestedObject;
			default:
				throw new IllegalStateException( "Unexpected value: " + fieldStructure.location );
		}
	}

	public <F> void initSingleValued(FieldTypeDescriptor<F, ?> fieldType, IndexFieldLocation location,
			DocumentElement document, F value) {
		initSingleValued( fieldType, location, document, value, null, false );
	}

	public <F> void initSingleValued(FieldTypeDescriptor<F, ?> fieldType, IndexFieldLocation location,
			DocumentElement document, F value, F garbageValue) {
		initSingleValued( fieldType, location, document, value, garbageValue, true );
	}

	public <F> void initSingleValued(FieldTypeDescriptor<F, ?> fieldType, IndexFieldLocation location,
			DocumentElement document, F value, F garbageValue, boolean includeGarbageValueInNested) {
		switch ( location ) {
			case ROOT:
				document.addValue( fieldWithSingleValueModels.get( fieldType ).reference, value );
				break;
			case IN_FLATTENED:
				DocumentElement flattenedObject0 = document.addObject( flattenedObject.self );
				flattenedObject0.addValue( flattenedObject.fieldWithSingleValueModels.get( fieldType ).reference,
						value
				);
				break;
			case IN_NESTED:
				// Make sure to create multiple nested documents here, to test all the scenarios.
				DocumentElement nestedObject0 = document.addObject( nestedObject.self );
				nestedObject0.addValue( nestedObject.discriminator, DISCRIMINATOR_VALUE_INCLUDED );
				nestedObject0.addValue(
						nestedObject.fieldWithSingleValueModels.get( fieldType ).reference,
						value
				);
				if ( includeGarbageValueInNested ) {
					DocumentElement nestedObject1 = document.addObject( nestedObject.self );
					nestedObject1.addValue( nestedObject.discriminator, DISCRIMINATOR_VALUE_EXCLUDED );
					nestedObject1.addValue(
							nestedObject.fieldWithSingleValueModels.get( fieldType ).reference,
							garbageValue
					);
				}
				break;
			case IN_NESTED_TWICE:
				// Same as for IN_NESTED, but one level deeper
				DocumentElement nestedObjectFirstLevel = document.addObject( nestedObject.self );
				DocumentElement nestedNestedObject0 = nestedObjectFirstLevel.addObject( nestedObject.nestedObject.self );
				nestedNestedObject0.addValue( nestedObject.nestedObject.discriminator, DISCRIMINATOR_VALUE_INCLUDED );
				nestedNestedObject0.addValue(
						nestedObject.nestedObject.fieldWithSingleValueModels.get( fieldType ).reference,
						value
				);
				if ( includeGarbageValueInNested ) {
					DocumentElement nestedNestedObject1 = nestedObjectFirstLevel.addObject( nestedObject.nestedObject.self );
					nestedNestedObject1.addValue( nestedObject.nestedObject.discriminator, DISCRIMINATOR_VALUE_EXCLUDED );
					nestedNestedObject1.addValue(
							nestedObject.nestedObject.fieldWithSingleValueModels.get( fieldType ).reference,
							garbageValue
					);
				}
				break;
		}
	}

	public <F> void initMultiValued(FieldTypeDescriptor<F, ?> fieldType, IndexFieldLocation location,
			DocumentElement document, List<F> values) {
		initMultiValued( fieldType, location, document, values, Collections.emptyList() );
	}

	public <F> void initMultiValued(FieldTypeDescriptor<F, ?> fieldType, IndexFieldLocation location,
			DocumentElement document, List<F> values, List<F> garbageValues) {
		switch ( location ) {
			case ROOT:
				for ( F value : values ) {
					document.addValue( fieldWithMultipleValuesModels.get( fieldType ).reference, value );
				}
				break;
			case IN_FLATTENED:
				DocumentElement flattenedObject0 = document.addObject( flattenedObject.self );
				for ( F value : values ) {
					flattenedObject0.addValue(
							flattenedObject.fieldWithMultipleValuesModels.get( fieldType ).reference,
							value
					);
				}
				break;
			case IN_NESTED:
				// The nested object requiring filters is split into four objects:
				// the first two are included by the filter and each hold part of the values that will be sorted on,
				// and the last two are excluded by the filter and hold garbage values that, if they were taken into account,
				// would mess with the sort order and eventually fail at least *some* tests.
				DocumentElement nestedObject0 = document.addObject( nestedObject.self );
				nestedObject0.addValue( nestedObject.discriminator, DISCRIMINATOR_VALUE_INCLUDED );
				if ( !values.isEmpty() ) {
					nestedObject0.addValue(
							nestedObject.fieldWithMultipleValuesModels.get( fieldType ).reference,
							values.get( 0 )
					);
				}
				DocumentElement nestedObject1 = document.addObject( nestedObject.self );
				nestedObject1.addValue( nestedObject.discriminator, DISCRIMINATOR_VALUE_INCLUDED );
				if ( values.size() > 1 ) {
					for ( F value : values.subList( 1, values.size() ) ) {
						nestedObject1.addValue(
								nestedObject.fieldWithMultipleValuesModels.get( fieldType ).reference,
								value
						);
					}
				}
				// This condition is necessary to avoid triggering https://github.com/elastic/elasticsearch/issues/92814
				if ( !garbageValues.isEmpty() ) {
					DocumentElement nestedObject2 = document.addObject( nestedObject.self );
					nestedObject2.addValue( nestedObject.discriminator, DISCRIMINATOR_VALUE_EXCLUDED );
					for ( F value : garbageValues ) {
						nestedObject2.addValue(
								nestedObject.fieldWithMultipleValuesModels.get( fieldType ).reference,
								value
						);
					}
					DocumentElement nestedObject3 = document.addObject( nestedObject.self );
					nestedObject3.addValue( nestedObject.discriminator, DISCRIMINATOR_VALUE_EXCLUDED );
					for ( F value : garbageValues ) {
						nestedObject3.addValue(
								nestedObject.fieldWithMultipleValuesModels.get( fieldType ).reference,
								value
						);
					}
				}
				break;
			case IN_NESTED_TWICE:
				// Same as for IN_NESTED, but one level deeper
				DocumentElement nestedObjectFirstLevel0 = document.addObject( nestedObject.self );
				DocumentElement nestedObjectFirstLevel1 = document.addObject( nestedObject.self );
				DocumentElement nestedNestedObject0 = nestedObjectFirstLevel0.addObject( nestedObject.nestedObject.self );
				nestedNestedObject0.addValue( nestedObject.nestedObject.discriminator, DISCRIMINATOR_VALUE_INCLUDED );
				if ( !values.isEmpty() ) {
					nestedNestedObject0.addValue(
							nestedObject.nestedObject.fieldWithMultipleValuesModels.get( fieldType ).reference,
							values.get( 0 )
					);
				}
				DocumentElement nestedNestedObject1 = nestedObjectFirstLevel1.addObject( nestedObject.nestedObject.self );
				nestedNestedObject1.addValue( nestedObject.nestedObject.discriminator, DISCRIMINATOR_VALUE_INCLUDED );
				if ( values.size() > 1 ) {
					for ( F value : values.subList( 1, values.size() ) ) {
						nestedNestedObject1.addValue(
								nestedObject.nestedObject.fieldWithMultipleValuesModels.get( fieldType ).reference,
								value
						);
					}
				}
				// This condition is necessary to avoid triggering https://github.com/elastic/elasticsearch/issues/92814
				if ( !garbageValues.isEmpty() ) {
					DocumentElement nestedNestedObject2 = nestedObjectFirstLevel0.addObject( nestedObject.nestedObject.self );
					nestedNestedObject2.addValue( nestedObject.nestedObject.discriminator, DISCRIMINATOR_VALUE_EXCLUDED );
					for ( F value : garbageValues ) {
						nestedNestedObject2.addValue(
								nestedObject.nestedObject.fieldWithMultipleValuesModels.get( fieldType ).reference,
								value
						);
					}
					DocumentElement nestedNestedObject3 = nestedObjectFirstLevel1.addObject( nestedObject.nestedObject.self );
					nestedNestedObject3.addValue( nestedObject.nestedObject.discriminator, DISCRIMINATOR_VALUE_EXCLUDED );
					for ( F value : garbageValues ) {
						nestedNestedObject3.addValue(
								nestedObject.nestedObject.fieldWithMultipleValuesModels.get( fieldType ).reference,
								value
						);
					}
				}
				break;
		}
	}
}
