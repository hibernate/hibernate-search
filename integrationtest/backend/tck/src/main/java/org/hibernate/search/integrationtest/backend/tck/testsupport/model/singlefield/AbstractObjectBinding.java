/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.model.singlefield;

import java.util.Collection;
import java.util.function.Consumer;

import org.hibernate.search.engine.backend.common.spi.FieldPaths;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.dsl.SearchableProjectableIndexFieldTypeOptionsStep;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModelsByType;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TestedFieldStructure;

public class AbstractObjectBinding {
	public static final String DISCRIMINATOR_FIELD_NAME = "discriminator";

	public final String absolutePath;
	public final String relativeFieldName;

	public final SimpleFieldModelsByType fieldWithSingleValueModels;
	public final SimpleFieldModelsByType fieldWithMultipleValuesModels;

	<S extends SearchableProjectableIndexFieldTypeOptionsStep<?, ?>> AbstractObjectBinding(AbstractObjectBinding parentBinding,
			String relativeFieldName, IndexSchemaElement self,
			Collection<? extends FieldTypeDescriptor<?, ? extends S>> supportedFieldTypes,
			Consumer<? super S> additionalConfiguration) {
		this.absolutePath = FieldPaths.compose( parentBinding == null ? null : parentBinding.absolutePath,
				relativeFieldName );
		this.relativeFieldName = relativeFieldName;
		fieldWithSingleValueModels = SimpleFieldModelsByType.mapAll( supportedFieldTypes, self,
				"", additionalConfiguration );
		fieldWithMultipleValuesModels = SimpleFieldModelsByType.mapAllMultiValued(
				supportedFieldTypes.stream().filter( FieldTypeDescriptor::isMultivaluable ), self,
				"multiValued_", additionalConfiguration );
	}

	public final String getRelativeFieldName(TestedFieldStructure fieldStructure, FieldTypeDescriptor<?, ?> fieldType) {
		SimpleFieldModelsByType fieldModelsByType;
		if ( fieldStructure.isSingleValued() ) {
			fieldModelsByType = fieldWithSingleValueModels;
		}
		else {
			fieldModelsByType = fieldWithMultipleValuesModels;
		}
		return fieldModelsByType.get( fieldType ).relativeFieldName;
	}

	protected final String getAbsoluteFieldPath(TestedFieldStructure fieldStructure, FieldTypeDescriptor<?, ?> fieldType) {
		return FieldPaths.compose( absolutePath, getRelativeFieldName( fieldStructure, fieldType ) );
	}

}
