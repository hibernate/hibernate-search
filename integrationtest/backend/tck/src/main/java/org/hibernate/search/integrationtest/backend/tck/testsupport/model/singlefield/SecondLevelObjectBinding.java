/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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

public class SecondLevelObjectBinding extends AbstractObjectBinding {
	public final IndexObjectFieldReference self;

	public final IndexFieldReference<String> discriminator;

	public static <S extends SearchableProjectableIndexFieldTypeOptionsStep<?, ?>> SecondLevelObjectBinding create(
			AbstractObjectBinding parentBinding, String relativeFieldName,
			IndexSchemaElement parent,
			ObjectStructure structure, Collection<? extends FieldTypeDescriptor<?, ? extends S>> supportedFieldTypes,
			Consumer<? super S> additionalConfiguration,
			IndexObjectFieldCardinality nestedFieldCardinality) {
		IndexSchemaObjectField objectField = parent.objectField( relativeFieldName, structure );
		if ( ObjectStructure.NESTED.equals( structure )
				&& IndexObjectFieldCardinality.MULTI_VALUED.equals( nestedFieldCardinality ) ) {
			objectField.multiValued();
		}
		return new SecondLevelObjectBinding( parentBinding, relativeFieldName, objectField,
				supportedFieldTypes, additionalConfiguration );
	}

	<S extends SearchableProjectableIndexFieldTypeOptionsStep<?, ?>> SecondLevelObjectBinding(
			AbstractObjectBinding parentBinding,
			String relativeFieldName,
			IndexSchemaObjectField objectField,
			Collection<? extends FieldTypeDescriptor<?, ? extends S>> supportedFieldTypes,
			Consumer<? super S> additionalConfiguration) {
		super( parentBinding, relativeFieldName, objectField, supportedFieldTypes, additionalConfiguration );
		self = objectField.toReference();
		discriminator = objectField.field( DISCRIMINATOR_FIELD_NAME, f -> f.asString() ).toReference();
	}
}
