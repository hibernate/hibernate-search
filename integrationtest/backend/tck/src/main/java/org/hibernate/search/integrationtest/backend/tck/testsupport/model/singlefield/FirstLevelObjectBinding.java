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
import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeOptionsStep;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;

public class FirstLevelObjectBinding extends AbstractObjectBinding {
	public final String relativeFieldName;
	public final IndexObjectFieldReference self;

	public final IndexFieldReference<String> discriminator;

	public final SecondLevelObjectBinding nestedObject;

	public static FirstLevelObjectBinding create(IndexSchemaElement parent, String relativeFieldName,
			ObjectFieldStorage storage, boolean multiValued,
			Collection<? extends FieldTypeDescriptor<?>> supportedFieldTypes,
			Consumer<StandardIndexFieldTypeOptionsStep<?, ?>> additionalConfiguration) {
		IndexSchemaObjectField objectField = parent.objectField( relativeFieldName, storage );
		if ( multiValued ) {
			objectField.multiValued();
		}
		return new FirstLevelObjectBinding( relativeFieldName, objectField,
				supportedFieldTypes, additionalConfiguration );
	}

	FirstLevelObjectBinding(String relativeFieldName, IndexSchemaObjectField objectField,
			Collection<? extends FieldTypeDescriptor<?>> supportedFieldTypes,
			Consumer<StandardIndexFieldTypeOptionsStep<?, ?>> additionalConfiguration) {
		super( objectField, supportedFieldTypes, additionalConfiguration );
		this.relativeFieldName = relativeFieldName;
		self = objectField.toReference();
		discriminator = objectField.field( "discriminator", f -> f.asString() ).toReference();
		nestedObject = SecondLevelObjectBinding.create(
				objectField, "nestedObject", ObjectFieldStorage.NESTED,
				supportedFieldTypes, additionalConfiguration
		);
	}
}
