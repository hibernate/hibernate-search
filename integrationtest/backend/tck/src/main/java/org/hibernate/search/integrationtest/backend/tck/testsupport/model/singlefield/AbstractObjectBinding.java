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
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeOptionsStep;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModelsByType;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TestedFieldStructure;

public class AbstractObjectBinding {
	public final String absolutePath;
	public final String relativeFieldName;

	public final SimpleFieldModelsByType fieldWithSingleValueModels;
	public final SimpleFieldModelsByType fieldWithMultipleValuesModels;

	AbstractObjectBinding(AbstractObjectBinding parentBinding, String relativeFieldName, IndexSchemaElement self,
			Collection<? extends FieldTypeDescriptor<?>> supportedFieldTypes,
			Consumer<StandardIndexFieldTypeOptionsStep<?, ?>> additionalConfiguration) {
		this.absolutePath = FieldPaths.compose( parentBinding == null ? null : parentBinding.absolutePath,
				relativeFieldName );
		this.relativeFieldName = relativeFieldName;
		fieldWithSingleValueModels = SimpleFieldModelsByType.mapAll( supportedFieldTypes, self,
				"", additionalConfiguration );
		fieldWithMultipleValuesModels = SimpleFieldModelsByType.mapAllMultiValued( supportedFieldTypes, self,
				"multiValued_", additionalConfiguration );
	}

	protected final String getRelativeFieldName(TestedFieldStructure fieldStructure, FieldTypeDescriptor<?> fieldType) {
		SimpleFieldModelsByType fieldModelsByType;
		if ( fieldStructure.isSingleValued() ) {
			fieldModelsByType = fieldWithSingleValueModels;
		}
		else {
			fieldModelsByType = fieldWithMultipleValuesModels;
		}
		return fieldModelsByType.get( fieldType ).relativeFieldName;
	}

	protected final String getAbsoluteFieldPath(TestedFieldStructure fieldStructure, FieldTypeDescriptor<?> fieldType) {
		return FieldPaths.compose( absolutePath, getRelativeFieldName( fieldStructure, fieldType ) );
	}

}
