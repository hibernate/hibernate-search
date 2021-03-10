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

	public AbstractPredicateFieldNestingIT(SimpleMappedIndex<IndexBinding> mainIndex,
			SimpleMappedIndex<MissingFieldIndexBinding> missingFieldIndex, DataSet<?, V> dataSet) {
		super( mainIndex, missingFieldIndex, dataSet );
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

		public void contribute(SimpleMappedIndex<IndexBinding> mainIndex, BulkIndexer mainIndexer,
				SimpleMappedIndex<MissingFieldIndexBinding> missingFieldIndex, BulkIndexer missingFieldIndexer) {
			F fieldValue = values.fieldValue( 0 );
			mainIndexer.add( docId( 0 ), routingKey,
					document -> mainIndex.binding().initDocument( document, fieldType, fieldValue ) );
			// Also add an empty document that shouldn't match
			mainIndexer.add( docId( 1 ), routingKey, document -> { } );

			missingFieldIndexer.add( docId( MISSING_FIELD_INDEX_DOC_ORDINAL ), routingKey,
					document -> missingFieldIndex.binding().initDocument() );
		}
	}
}
