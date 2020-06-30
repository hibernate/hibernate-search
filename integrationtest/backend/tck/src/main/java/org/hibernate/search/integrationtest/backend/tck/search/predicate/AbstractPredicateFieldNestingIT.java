/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;

public abstract class AbstractPredicateFieldNestingIT<V extends AbstractPredicateTestValues<?>>
		extends AbstractPredicateNestingIT {

	protected final DataSet<?, V> dataSet;

	public AbstractPredicateFieldNestingIT(SimpleMappedIndex<IndexBinding> index, DataSet<?, V> dataSet) {
		super( index, dataSet );
		this.dataSet = dataSet;
	}

	@Override
	protected final PredicateFinalStep predicate(SearchPredicateFactory f, ObjectFieldBinding objectFieldBinding,
			int matchingDocOrdinal) {
		return predicate( f, objectFieldBinding.fieldPath( dataSet.fieldType ), matchingDocOrdinal );
	}

	protected abstract PredicateFinalStep predicate(SearchPredicateFactory f, String fieldPath, int matchingDocOrdinal);

	public static final class DataSet<F, V extends AbstractPredicateTestValues<F>>
			extends AbstractPerFieldTypePredicateDataSet<F, V> {
		public DataSet(V values) {
			super( values );
		}

		public void contribute(SimpleMappedIndex<IndexBinding> index, BulkIndexer indexer) {
			F fieldValue = values.fieldValue( 0 );
			indexer.add( docId( 0 ), routingKey,
					document -> index.binding().initDocument( document, fieldType, fieldValue ) );
			// Also add an empty document that shouldn't match
			indexer.add( docId( 1 ), routingKey, document -> { } );
		}
	}
}
