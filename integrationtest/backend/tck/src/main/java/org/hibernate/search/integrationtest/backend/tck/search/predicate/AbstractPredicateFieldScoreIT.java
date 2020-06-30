/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;

import java.util.Collection;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModelsByType;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;

import org.junit.Test;

public abstract class AbstractPredicateFieldScoreIT<V extends AbstractPredicateTestValues<?>>
		extends AbstractPredicateScoreIT {
	private final SimpleMappedIndex<IndexBinding> index;
	protected final DataSet<?, V> dataSet;

	public AbstractPredicateFieldScoreIT(SimpleMappedIndex<IndexBinding> index, DataSet<?, V> dataSet) {
		super( index, dataSet );
		this.index = index;
		this.dataSet = dataSet;
	}

	@Test
	public void fieldLevelBoost() {
		assertThatQuery( index.query()
				.where( f -> f.bool()
						.should( predicate( f, field0Path(), 0 ) )
						.should( predicateWithFieldLevelBoost( f, field0Path(), 42f, 1 ) ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.docId( 1 ), dataSet.docId( 0 ) );

		assertThatQuery( index.query()
				.where( f -> f.bool()
						.should( predicateWithFieldLevelBoost( f, field0Path(), 42f,
								0 ) )
						.should( predicate( f, field0Path(), 1 ) ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.docId( 0 ), dataSet.docId( 1 ) );
	}

	@Test
	public void predicateLevelBoost_fieldLevelBoost() {
		assertThatQuery( index.query()
				.where( f -> f.bool()
						// 2 * 2 => boost x4
						.should( predicateWithFieldLevelBoostAndPredicateLevelBoost( f, field0Path(), 2f,
								0, 2f ) )
						// 3 * 3 => boost x9
						.should( predicateWithFieldLevelBoostAndPredicateLevelBoost( f, field0Path(), 3f,
								1, 3f ) ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.docId( 1 ), dataSet.docId( 0 ) );

		assertThatQuery( index.query()
				.where( f -> f.bool()
						// 1 * 3 => boost x3
						.should( predicateWithFieldLevelBoostAndPredicateLevelBoost( f, field0Path(), 1f,
								0, 3f ) )
						// 0.1 * 3 => boost x0.3
						.should( predicateWithFieldLevelBoostAndPredicateLevelBoost( f, field0Path(), 0.1f,
								1, 3f ) ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.docId( 0 ), dataSet.docId( 1 ) );
	}

	@Test
	public void constantScore_fieldLevelBoost() {
		assumeConstantScoreSupported();

		SearchPredicateFactory f = index.createScope().predicate();

		assertThatThrownBy( () -> predicateWithFieldLevelBoostAndConstantScore( f, field0Path(), 2.1f,
				0 )
				.toPredicate() )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "per-field boosts together with withConstantScore option" );
	}

	@Test
	public void predicateLevelBoost_multiFields() {
		assertThatQuery( index.query()
				.where( f -> f.bool()
						.should( predicateWithPredicateLevelBoost( f, new String[] { field0Path(), field1Path() },
								0, 7f ) )
						.should( predicateWithPredicateLevelBoost( f, new String[] { field0Path(), field1Path() },
								1, 39f ) ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.docId( 1 ), dataSet.docId( 0 ) );

		assertThatQuery( index.query()
				.where( f -> f.bool()
						.should( predicateWithPredicateLevelBoost( f, new String[] { field0Path(), field1Path() },
								0, 39f ) )
						.should( predicateWithPredicateLevelBoost( f, new String[] { field0Path(), field1Path() },
								1, 7f ) ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.docId( 0 ), dataSet.docId( 1 ) );
	}

	@Override
	protected final PredicateFinalStep predicate(SearchPredicateFactory f, int matchingDocOrdinal) {
		return predicate( f, field0Path(), matchingDocOrdinal );
	}

	@Override
	protected final PredicateFinalStep predicateWithBoost(SearchPredicateFactory f, int matchingDocOrdinal, float boost) {
		return predicateWithPredicateLevelBoost( f, new String[] { field0Path() },
				matchingDocOrdinal, boost );
	}

	@Override
	protected final PredicateFinalStep predicateWithConstantScore(SearchPredicateFactory f, int matchingDocOrdinal) {
		return predicateWithConstantScore( f, new String[] { field0Path() },
				matchingDocOrdinal );
	}

	@Override
	protected final PredicateFinalStep predicateWithConstantScoreAndBoost(SearchPredicateFactory f,
			int matchingDocOrdinal, float boost) {
		return predicateWithConstantScoreAndPredicateLevelBoost( f, new String[] { field0Path() },
				matchingDocOrdinal, boost );
	}

	protected abstract PredicateFinalStep predicate(SearchPredicateFactory f, String fieldPath,
			int matchingDocOrdinal);

	protected abstract PredicateFinalStep predicateWithConstantScore(SearchPredicateFactory f,
			String[] fieldPaths, int matchingDocOrdinal);

	protected abstract PredicateFinalStep predicateWithPredicateLevelBoost(SearchPredicateFactory f,
			String[] fieldPaths, int matchingDocOrdinal, float predicateBoost);

	protected abstract PredicateFinalStep predicateWithConstantScoreAndPredicateLevelBoost(SearchPredicateFactory f,
			String[] fieldPaths, int matchingDocOrdinal, float predicateBoost);

	protected abstract PredicateFinalStep predicateWithFieldLevelBoost(SearchPredicateFactory f,
			String fieldPath, float fieldBoost, int matchingDocOrdinal);

	protected abstract PredicateFinalStep predicateWithFieldLevelBoostAndPredicateLevelBoost(SearchPredicateFactory f,
			String fieldPath, float fieldBoost, int matchingDocOrdinal, float predicateBoost);

	protected abstract PredicateFinalStep predicateWithFieldLevelBoostAndConstantScore(SearchPredicateFactory f,
			String fieldPath, float fieldBoost, int matchingDocOrdinal);

	private String field0Path() {
		return index.binding().field0.get( dataSet.fieldType ).relativeFieldName;
	}

	private String field1Path() {
		return index.binding().field1.get( dataSet.fieldType ).relativeFieldName;
	}

	public static final class IndexBinding {
		private final SimpleFieldModelsByType field0;
		private final SimpleFieldModelsByType field1;

		public IndexBinding(IndexSchemaElement root, Collection<? extends FieldTypeDescriptor<?>> fieldTypes) {
			field0 = SimpleFieldModelsByType.mapAll( fieldTypes, root, "field0_" );
			field1 = SimpleFieldModelsByType.mapAll( fieldTypes, root, "field1_" );
		}
	}

	public static final class DataSet<F, V extends AbstractPredicateTestValues<F>>
			extends AbstractPerFieldTypePredicateDataSet<F, V> {
		public DataSet(V values) {
			super( values );
		}

		public void contribute(SimpleMappedIndex<IndexBinding> index, BulkIndexer indexer) {
			indexer.add( docId( 0 ), routingKey,
					document -> initDocument( index, document, values.fieldValue( 0 ) ) );
			indexer.add( docId( 1 ), routingKey,
					document -> initDocument( index, document, values.fieldValue( 1 ) ) );
		}

		private void initDocument(SimpleMappedIndex<IndexBinding> index, DocumentElement document, F fieldValue) {
			IndexBinding binding = index.binding();
			document.addValue( binding.field0.get( fieldType ).reference, fieldValue );
			document.addValue( binding.field1.get( fieldType ).reference, fieldValue );
		}
	}
}
