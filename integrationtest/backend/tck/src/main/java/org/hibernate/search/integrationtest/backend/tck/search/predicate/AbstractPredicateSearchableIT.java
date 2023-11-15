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
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.engine.backend.types.dsl.SearchableProjectableIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModelsByType;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public abstract class AbstractPredicateSearchableIT {

	@ParameterizedTest(name = "{2}")
	@MethodSource("params")
	void unsearchable(SimpleMappedIndex<SearchableYesIndexBinding> searchableYesIndex,
			SimpleMappedIndex<SearchableNoIndexBinding> searchableNoIndex,
			FieldTypeDescriptor<?, ?> fieldType) {
		SearchPredicateFactory f = searchableNoIndex.createScope().predicate();

		String fieldPath = searchableNoIndex.binding().field.get( fieldType ).relativeFieldName;

		assertThatThrownBy( () -> tryPredicate( f, fieldPath, fieldType ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Cannot use '" + predicateNameInErrorMessage() + "' on field '" + fieldPath + "'",
						"Make sure the field is marked as searchable/sortable/projectable/aggregable/highlightable (whichever is relevant)"
				);
	}

	@ParameterizedTest(name = "{2}")
	@MethodSource("params")
	void multiIndex_incompatibleSearchable(SimpleMappedIndex<SearchableYesIndexBinding> searchableYesIndex,
			SimpleMappedIndex<SearchableNoIndexBinding> searchableNoIndex,
			FieldTypeDescriptor<?, ?> fieldType) {
		SearchPredicateFactory f = searchableYesIndex.createScope( searchableNoIndex ).predicate();

		String fieldPath = searchableYesIndex.binding().field.get( fieldType ).relativeFieldName;

		assertThatThrownBy( () -> tryPredicate( f, fieldPath, fieldType ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Inconsistent configuration for field '" + fieldPath + "' in a search query across multiple indexes",
						"Inconsistent support for '" + predicateNameInErrorMessage() + "'"
				);
	}

	protected abstract void tryPredicate(SearchPredicateFactory f, String fieldPath,
			FieldTypeDescriptor<?, ?> fieldType);

	protected abstract String predicateNameInErrorMessage();

	public static final class SearchableYesIndexBinding {
		private final SimpleFieldModelsByType field;

		public SearchableYesIndexBinding(IndexSchemaElement root, Collection<
				? extends FieldTypeDescriptor<?, ? extends SearchableProjectableIndexFieldTypeOptionsStep<?, ?>>> fieldTypes) {
			field = SimpleFieldModelsByType.mapAll( fieldTypes, root, "", c -> c.searchable( Searchable.YES ) );
		}
	}

	public static final class SearchableNoIndexBinding {
		private final SimpleFieldModelsByType field;

		public SearchableNoIndexBinding(IndexSchemaElement root, Collection<
				? extends FieldTypeDescriptor<?, ? extends SearchableProjectableIndexFieldTypeOptionsStep<?, ?>>> fieldTypes) {
			field = SimpleFieldModelsByType.mapAll( fieldTypes, root, "", c -> c.searchable( Searchable.NO ) );
		}
	}

}
