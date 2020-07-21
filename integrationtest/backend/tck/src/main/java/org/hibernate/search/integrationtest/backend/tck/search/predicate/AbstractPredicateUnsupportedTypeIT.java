/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collection;

import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModelsByType;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;

import org.junit.Test;

public abstract class AbstractPredicateUnsupportedTypeIT {

	private final SimpleMappedIndex<IndexBinding> index;
	private final FieldTypeDescriptor<?> fieldType;

	protected AbstractPredicateUnsupportedTypeIT(SimpleMappedIndex<IndexBinding> index, FieldTypeDescriptor<?> fieldType) {
		this.index = index;
		this.fieldType = fieldType;
	}

	@Test
	public void unsupported() {
		SearchPredicateFactory f = index.createScope().predicate();

		String fieldPath = index.binding().field.get( fieldType ).relativeFieldName;

		assertThatThrownBy( () -> tryPredicate( f, fieldPath ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Cannot use '" + predicateNameInErrorMessage() + "' on field '" + fieldPath + "'",
						"'" + predicateNameInErrorMessage() + "' is not available for fields of this type"
				)
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexFieldAbsolutePath( fieldPath )
				) );
	}

	protected abstract void tryPredicate(SearchPredicateFactory f, String fieldPath);

	protected abstract String predicateNameInErrorMessage();

	public static final class IndexBinding {
		private final SimpleFieldModelsByType field;

		public IndexBinding(IndexSchemaElement root, Collection<? extends FieldTypeDescriptor<?>> fieldTypes) {
			field = SimpleFieldModelsByType.mapAll( fieldTypes, root, "" );
		}
	}
}
