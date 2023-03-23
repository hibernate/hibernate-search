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
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModelsByType;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;

import org.junit.Test;

public abstract class AbstractPredicateSearchableIT {

	private final SimpleMappedIndex<SearchableYesIndexBinding> searchableYesIndex;
	private final SimpleMappedIndex<SearchableNoIndexBinding> searchableNoIndex;
	private final FieldTypeDescriptor<?> fieldType;

	protected AbstractPredicateSearchableIT(SimpleMappedIndex<SearchableYesIndexBinding> searchableYesIndex,
			SimpleMappedIndex<SearchableNoIndexBinding> searchableNoIndex,
			FieldTypeDescriptor<?> fieldType) {
		this.searchableYesIndex = searchableYesIndex;
		this.searchableNoIndex = searchableNoIndex;
		this.fieldType = fieldType;
	}

	@Test
	public void unsearchable() {
		SearchPredicateFactory f = searchableNoIndex.createScope().predicate();

		String fieldPath = searchableNoIndex.binding().field.get( fieldType ).relativeFieldName;

		assertThatThrownBy( () -> tryPredicate( f, fieldPath ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Cannot use '" + predicateNameInErrorMessage() + "' on field '" + fieldPath + "'",
						"Make sure the field is marked as searchable/sortable/projectable/aggregable/highlightable (whichever is relevant)"
				);
	}

	@Test
	public void multiIndex_incompatibleSearchable() {
		SearchPredicateFactory f = searchableYesIndex.createScope( searchableNoIndex ).predicate();

		String fieldPath = searchableYesIndex.binding().field.get( fieldType ).relativeFieldName;

		assertThatThrownBy( () -> tryPredicate( f, fieldPath ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Inconsistent configuration for field '" + fieldPath + "' in a search query across multiple indexes",
						"Inconsistent support for '" + predicateNameInErrorMessage() + "'"
				);
	}

	protected abstract void tryPredicate(SearchPredicateFactory f, String fieldPath);

	protected abstract String predicateNameInErrorMessage();

	public static final class SearchableYesIndexBinding {
		private final SimpleFieldModelsByType field;

		public SearchableYesIndexBinding(IndexSchemaElement root, Collection<? extends FieldTypeDescriptor<?>> fieldTypes) {
			field = SimpleFieldModelsByType.mapAll( fieldTypes, root, "", c -> c.searchable( Searchable.YES ) );
		}
	}

	public static final class SearchableNoIndexBinding {
		private final SimpleFieldModelsByType field;

		public SearchableNoIndexBinding(IndexSchemaElement root, Collection<? extends FieldTypeDescriptor<?>> fieldTypes) {
			field = SimpleFieldModelsByType.mapAll( fieldTypes, root, "", c -> c.searchable( Searchable.NO ) );
		}
	}

}
