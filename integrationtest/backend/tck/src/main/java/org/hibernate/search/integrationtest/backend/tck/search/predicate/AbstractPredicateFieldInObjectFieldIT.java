/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;

import org.hibernate.search.engine.search.predicate.definition.PredicateDefinition;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public abstract class AbstractPredicateFieldInObjectFieldIT<V extends AbstractPredicateTestValues<?>>
		extends AbstractPredicateInObjectFieldIT {

	@ParameterizedTest(name = "{2}")
	@MethodSource("params")
	@TestForIssue(jiraKey = "HSEARCH-4162")
	void factoryWithRoot_nested(SimpleMappedIndex<IndexBinding> mainIndex,
			SimpleMappedIndex<MissingFieldIndexBinding> missingFieldIndex, DataSet<?, V> dataSet) {
		assertThatQuery( mainIndex.query()
				.where( f -> predicateWithRelativePath( f.withRoot( mainIndex.binding().nested.absolutePath ),
						mainIndex.binding().nested, 0,
						dataSet
				) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), dataSet.docId( 0 ) );
	}

	@ParameterizedTest(name = "{2}")
	@MethodSource("params")
	@TestForIssue(jiraKey = "HSEARCH-4162")
	void factoryWithRoot_flattened(SimpleMappedIndex<IndexBinding> mainIndex,
			SimpleMappedIndex<MissingFieldIndexBinding> missingFieldIndex, DataSet<?, V> dataSet) {
		assertThatQuery( mainIndex.query()
				.where( f -> predicateWithRelativePath( f.withRoot( mainIndex.binding().flattened.absolutePath ),
						mainIndex.binding().flattened, 0,
						dataSet
				) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), dataSet.docId( 0 ) );
	}

	@ParameterizedTest(name = "{2}")
	@MethodSource("params")
	@TestForIssue(jiraKey = "HSEARCH-4162")
	void inNamedPredicate_nested(SimpleMappedIndex<IndexBinding> mainIndex,
			SimpleMappedIndex<MissingFieldIndexBinding> missingFieldIndex, DataSet<?, V> dataSet) {
		assertThatQuery( mainIndex.query()
				.where( f -> f.named( mainIndex.binding().nested.absolutePath + "." + StubPredicateDefinition.NAME )
						.param( StubPredicateDefinition.IMPL_PARAM_NAME,
								(PredicateDefinition) context -> predicateWithRelativePath( context.predicate(),
										mainIndex.binding().nested, 0, dataSet )
										.toPredicate() ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), dataSet.docId( 0 ) );
	}

	@ParameterizedTest(name = "{2}")
	@MethodSource("params")
	@TestForIssue(jiraKey = "HSEARCH-4162")
	void inNamedPredicate_flattened(SimpleMappedIndex<IndexBinding> mainIndex,
			SimpleMappedIndex<MissingFieldIndexBinding> missingFieldIndex, DataSet<?, V> dataSet) {
		assertThatQuery( mainIndex.query()
				.where( f -> f.named( mainIndex.binding().flattened.absolutePath + "." + StubPredicateDefinition.NAME )
						.param( StubPredicateDefinition.IMPL_PARAM_NAME,
								(PredicateDefinition) context -> predicateWithRelativePath( context.predicate(),
										mainIndex.binding().flattened, 0,
										dataSet
								)
										.toPredicate() ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), dataSet.docId( 0 ) );
	}

	@Override
	@SuppressWarnings("unchecked")
	protected final PredicateFinalStep predicate(SearchPredicateFactory f, ObjectFieldBinding objectFieldBinding,
			int matchingDocOrdinal, AbstractPredicateDataSet dataSet) {
		return predicate( f, objectFieldBinding.absoluteFieldPath( ( (DataSet<?, V>) dataSet ).fieldType ), matchingDocOrdinal,
				(DataSet<?, V>) dataSet
		);
	}

	protected final PredicateFinalStep predicateWithRelativePath(SearchPredicateFactory f,
			ObjectFieldBinding objectFieldBinding,
			int matchingDocOrdinal, DataSet<?, V> dataSet) {
		return predicate( f, objectFieldBinding.relativeFieldPath( dataSet.fieldType ), matchingDocOrdinal, dataSet );
	}

	protected abstract PredicateFinalStep predicate(SearchPredicateFactory f, String fieldPath, int matchingDocOrdinal,
			DataSet<?, V> dataSet);

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
			mainIndexer.add( docId( 1 ), routingKey, document -> {} );

			missingFieldIndexer.add( docId( MISSING_FIELD_INDEX_DOC_ORDINAL ), routingKey,
					document -> missingFieldIndex.binding().initDocument() );
		}
	}
}
