/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;

import java.util.Collection;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModelsByType;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;

import org.junit.jupiter.api.Test;

// TODO HSEARCH-3593 test multiple field structures (in nested, ...)
public abstract class AbstractPredicateSingleFieldIT<V extends AbstractPredicateTestValues<?>> {

	private final SimpleMappedIndex<IndexBinding> index;
	protected final DataSet<?, V> dataSet;

	protected AbstractPredicateSingleFieldIT(SimpleMappedIndex<IndexBinding> index, DataSet<?, V> dataSet) {
		this.index = index;
		this.dataSet = dataSet;
	}

	@Test
	public void match() {
		int valueCount = dataSet.values.size();
		for ( int i = 0; i < valueCount; i++ ) {
			int matchingDocOrdinal = i;
			assertThatQuery( index.query()
					.where( f -> predicate( f, fieldPath(), matchingDocOrdinal ) )
					.routing( dataSet.routingKey ) )
					.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( i ) );
		}
	}

	protected abstract PredicateFinalStep predicate(SearchPredicateFactory f, String fieldPath, int matchingDocOrdinal);

	private String fieldPath() {
		return index.binding().field.get( dataSet.fieldType ).relativeFieldName;
	}

	public static final class IndexBinding {
		private final SimpleFieldModelsByType field;

		public IndexBinding(IndexSchemaElement root, Collection<? extends FieldTypeDescriptor<?>> fieldTypes) {
			field = SimpleFieldModelsByType.mapAll( fieldTypes, root, "field0_" );
		}
	}

	public static final class DataSet<F, V extends AbstractPredicateTestValues<F>>
			extends AbstractPerFieldTypePredicateDataSet<F, V> {
		public DataSet(V values) {
			super( values );
		}

		public void contribute(SimpleMappedIndex<IndexBinding> index, BulkIndexer indexer) {
			int valueCount = values.size();
			for ( int i = 0; i < valueCount; i++ ) {
				F fieldValue = values.fieldValue( i );
				indexer.add( docId( i ), routingKey, document -> initDocument( index, document, fieldValue ) );
			}
		}

		private void initDocument(SimpleMappedIndex<IndexBinding> index, DocumentElement document,
				F fieldValue) {
			IndexBinding binding = index.binding();
			document.addValue( binding.field.get( fieldType ).reference, fieldValue );
		}
	}
}
