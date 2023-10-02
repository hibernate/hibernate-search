/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;

import java.util.Collection;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.dsl.SearchableProjectableIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModelsByType;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public abstract class AbstractPredicateFieldScoreIT<V extends AbstractPredicateTestValues<?>>
		extends AbstractPredicateScoreIT {
	@ParameterizedTest(name = "{1}")
	@MethodSource("params")
	void fieldLevelBoost(SimpleMappedIndex<IndexBinding> index, DataSet<?, V> dataSet) {
		assertThatQuery( index.query()
				.where( f -> f.or(
						predicate( f, field0Path( index, dataSet ), 0, dataSet ),
						predicateWithFieldLevelBoost( f, field0Path( index, dataSet ), 42f, 1, dataSet ) ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.docId( 1 ), dataSet.docId( 0 ) );

		assertThatQuery( index.query()
				.where( f -> f.or(
						predicateWithFieldLevelBoost( f, field0Path( index, dataSet ), 42f, 0, dataSet ),
						predicate( f, field0Path( index, dataSet ), 1, dataSet ) ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.docId( 0 ), dataSet.docId( 1 ) );
	}

	@ParameterizedTest(name = "{1}")
	@MethodSource("params")
	void predicateLevelBoost_fieldLevelBoost(SimpleMappedIndex<IndexBinding> index, DataSet<?, V> dataSet) {
		assertThatQuery( index.query()
				.where( f -> f.or(
						// 2 * 2 => boost x4
						predicateWithFieldLevelBoostAndPredicateLevelBoost( f, field0Path( index, dataSet ), 2f,
								0, 2f, dataSet
						),
						// 3 * 3 => boost x9
						predicateWithFieldLevelBoostAndPredicateLevelBoost( f, field0Path( index, dataSet ), 3f,
								1, 3f, dataSet
						) ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.docId( 1 ), dataSet.docId( 0 ) );

		assertThatQuery( index.query()
				.where( f -> f.or(
						// 1 * 3 => boost x3
						predicateWithFieldLevelBoostAndPredicateLevelBoost( f, field0Path( index, dataSet ), 1f,
								0, 3f, dataSet
						),
						// 0.1 * 3 => boost x0.3
						predicateWithFieldLevelBoostAndPredicateLevelBoost( f, field0Path( index, dataSet ), 0.1f,
								1, 3f, dataSet
						) ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.docId( 0 ), dataSet.docId( 1 ) );
	}

	@ParameterizedTest(name = "{1}")
	@MethodSource("params")
	void constantScore_fieldLevelBoost(SimpleMappedIndex<IndexBinding> index, DataSet<?, V> dataSet) {
		assumeConstantScoreSupported();

		SearchPredicateFactory f = index.createScope().predicate();

		assertThatThrownBy( () -> predicateWithFieldLevelBoostAndConstantScore( f, field0Path( index, dataSet ), 2.1f,
				0, dataSet
		)
				.toPredicate() )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Invalid use of per-field boost: the predicate score is constant.",
						"Cannot assign a different boost to each field when the predicate score is constant." );
	}

	@ParameterizedTest(name = "{1}")
	@MethodSource("params")
	void predicateLevelBoost_multiFields(SimpleMappedIndex<IndexBinding> index, DataSet<?, V> dataSet) {
		assertThatQuery( index.query()
				.where( f -> f.or(
						predicateWithPredicateLevelBoost( f, new String[] {
								field0Path( index, dataSet ),
								field1Path(
										index, dataSet ) },
								0, 7f, dataSet
						),
						predicateWithPredicateLevelBoost( f, new String[] {
								field0Path( index, dataSet ),
								field1Path(
										index, dataSet ) },
								1, 39f, dataSet
						) ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.docId( 1 ), dataSet.docId( 0 ) );

		assertThatQuery( index.query()
				.where( f -> f.or(
						predicateWithPredicateLevelBoost( f, new String[] {
								field0Path( index, dataSet ),
								field1Path(
										index, dataSet ) },
								0, 39f, dataSet
						),
						predicateWithPredicateLevelBoost( f, new String[] {
								field0Path( index, dataSet ),
								field1Path(
										index, dataSet ) },
								1, 7f, dataSet
						) ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.docId( 0 ), dataSet.docId( 1 ) );
	}

	@SuppressWarnings("unchecked")
	@Override
	protected final PredicateFinalStep predicate(SearchPredicateFactory f, int matchingDocOrdinal,
			AbstractPredicateDataSet dataSet, StubMappedIndex index) {
		return predicate(
				f, field0Path( (SimpleMappedIndex<IndexBinding>) index, (DataSet<?, V>) dataSet ), matchingDocOrdinal,
				(DataSet<?, V>) dataSet
		);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected final PredicateFinalStep predicateWithBoost(SearchPredicateFactory f, int matchingDocOrdinal, float boost,
			AbstractPredicateDataSet dataSet, StubMappedIndex index) {
		return predicateWithPredicateLevelBoost( f, new String[] {
				field0Path(
						(SimpleMappedIndex<IndexBinding>) index,
						(DataSet<?, V>) dataSet
				)
		},
				matchingDocOrdinal, boost, (DataSet<?, V>) dataSet
		);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected final PredicateFinalStep predicateWithConstantScore(SearchPredicateFactory f, int matchingDocOrdinal,
			AbstractPredicateDataSet dataSet, StubMappedIndex index) {
		return predicateWithConstantScore( f,
				new String[] { field0Path( (SimpleMappedIndex<IndexBinding>) index, (DataSet<?, V>) dataSet ) },
				matchingDocOrdinal, (DataSet<?, V>) dataSet
		);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected final PredicateFinalStep predicateWithConstantScoreAndBoost(SearchPredicateFactory f,
			int matchingDocOrdinal, float boost, AbstractPredicateDataSet dataSet,
			StubMappedIndex index) {
		return predicateWithConstantScoreAndPredicateLevelBoost( f,
				new String[] { field0Path( (SimpleMappedIndex<IndexBinding>) index, (DataSet<?, V>) dataSet ) },
				matchingDocOrdinal, boost, (DataSet<?, V>) dataSet
		);
	}

	protected abstract PredicateFinalStep predicate(SearchPredicateFactory f, String fieldPath,
			int matchingDocOrdinal, DataSet<?, V> dataSet);

	protected abstract PredicateFinalStep predicateWithConstantScore(SearchPredicateFactory f,
			String[] fieldPaths, int matchingDocOrdinal, DataSet<?, V> dataSet);

	protected abstract PredicateFinalStep predicateWithPredicateLevelBoost(SearchPredicateFactory f,
			String[] fieldPaths, int matchingDocOrdinal, float predicateBoost,
			DataSet<?, V> dataSet);

	protected abstract PredicateFinalStep predicateWithConstantScoreAndPredicateLevelBoost(SearchPredicateFactory f,
			String[] fieldPaths, int matchingDocOrdinal, float predicateBoost,
			DataSet<?, V> dataSet);

	protected abstract PredicateFinalStep predicateWithFieldLevelBoost(SearchPredicateFactory f,
			String fieldPath, float fieldBoost, int matchingDocOrdinal, DataSet<?, V> dataSet);

	protected abstract PredicateFinalStep predicateWithFieldLevelBoostAndPredicateLevelBoost(SearchPredicateFactory f,
			String fieldPath, float fieldBoost, int matchingDocOrdinal, float predicateBoost,
			DataSet<?, V> dataSet);

	protected abstract PredicateFinalStep predicateWithFieldLevelBoostAndConstantScore(SearchPredicateFactory f,
			String fieldPath, float fieldBoost, int matchingDocOrdinal, DataSet<?, V> dataSet);

	private String field0Path(SimpleMappedIndex<IndexBinding> index, DataSet<?, V> dataSet) {
		return index.binding().field0.get( dataSet.fieldType ).relativeFieldName;
	}

	private String field1Path(SimpleMappedIndex<IndexBinding> index, DataSet<?, V> dataSet) {
		return index.binding().field1.get( dataSet.fieldType ).relativeFieldName;
	}

	public static final class IndexBinding {
		private final SimpleFieldModelsByType field0;
		private final SimpleFieldModelsByType field1;

		public IndexBinding(IndexSchemaElement root, Collection<
				? extends FieldTypeDescriptor<?, ? extends SearchableProjectableIndexFieldTypeOptionsStep<?, ?>>> fieldTypes) {
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
