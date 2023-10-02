/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.model.singlefield;

import java.util.Collection;
import java.util.function.Consumer;

import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.backend.types.dsl.SearchableProjectableIndexFieldTypeOptionsStep;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.IndexObjectFieldCardinality;

public class FirstLevelObjectBinding extends AbstractObjectBinding {
	public final IndexObjectFieldReference self;

	public final IndexFieldReference<String> discriminator;

	public final SecondLevelObjectBinding nestedObject;

	public static <S extends SearchableProjectableIndexFieldTypeOptionsStep<?, ?>> FirstLevelObjectBinding create(
			AbstractObjectBinding parentBinding, String relativeFieldName,
			IndexSchemaElement parent,
			ObjectStructure structure,
			Collection<? extends FieldTypeDescriptor<?, ? extends S>> supportedFieldTypes,
			Consumer<? super S> additionalConfiguration,
			IndexObjectFieldCardinality nestedFieldCardinality) {
		IndexSchemaObjectField objectField = parent.objectField( relativeFieldName, structure );
		if ( ObjectStructure.NESTED.equals( structure )
				&& IndexObjectFieldCardinality.MULTI_VALUED.equals( nestedFieldCardinality ) ) {
			objectField.multiValued();
		}
		return new FirstLevelObjectBinding( parentBinding, relativeFieldName, objectField, supportedFieldTypes,
				additionalConfiguration, nestedFieldCardinality );
	}

	<S extends SearchableProjectableIndexFieldTypeOptionsStep<?, ?>> FirstLevelObjectBinding(
			AbstractObjectBinding parentBinding,
			String relativeFieldName,
			IndexSchemaObjectField objectField,
			Collection<? extends FieldTypeDescriptor<?, ? extends S>> supportedFieldTypes,
			Consumer<? super S> additionalConfiguration,
			IndexObjectFieldCardinality nestedFieldCardinality) {
		super( parentBinding, relativeFieldName, objectField, supportedFieldTypes, additionalConfiguration );
		self = objectField.toReference();
		discriminator = objectField.field( DISCRIMINATOR_FIELD_NAME, f -> f.asString() ).toReference();
		nestedObject = SecondLevelObjectBinding.create(
				this, "nestedObject", objectField, ObjectStructure.NESTED, supportedFieldTypes,
				additionalConfiguration, nestedFieldCardinality
		);
	}
}
