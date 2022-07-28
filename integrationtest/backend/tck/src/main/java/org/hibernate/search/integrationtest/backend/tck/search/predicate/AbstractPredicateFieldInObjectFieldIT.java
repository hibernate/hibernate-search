/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;

import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.definition.PredicateDefinition;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Test;

public abstract class AbstractPredicateFieldInObjectFieldIT<V extends AbstractPredicateTestValues<?>>
		extends AbstractPredicateInObjectFieldIT {

	protected final DataSet<?, V> dataSet;

	public AbstractPredicateFieldInObjectFieldIT(SimpleMappedIndex<IndexBinding> mainIndex,
			SimpleMappedIndex<MissingFieldIndexBinding> missingFieldIndex, DataSet<?, V> dataSet) {
		super( mainIndex, missingFieldIndex, dataSet );
		this.dataSet = dataSet;
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4162")
	public void factoryWithRoot_nested() {
		assertThatQuery( mainIndex.query()
				.where( f -> predicateWithRelativePath( f.withRoot( binding.nested.absolutePath ), binding.nested, 0 ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), dataSet.docId( 0 ) );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4162")
	public void factoryWithRoot_flattened() {
		assertThatQuery( mainIndex.query()
				.where( f -> predicateWithRelativePath( f.withRoot( binding.flattened.absolutePath ), binding.flattened, 0 ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), dataSet.docId( 0 ) );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4162")
	public void inNamedPredicate_nested() {
		assertThatQuery( mainIndex.query()
				.where( f -> f.named( binding.nested.absolutePath + "." + StubPredicateDefinition.NAME )
						.param( StubPredicateDefinition.IMPL_PARAM_NAME, (PredicateDefinition) context ->
								predicateWithRelativePath( context.predicate(), binding.nested, 0 )
										.toPredicate() ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), dataSet.docId( 0 ) );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4162")
	public void inNamedPredicate_flattened() {
		assertThatQuery( mainIndex.query()
				.where( f -> f.named( binding.flattened.absolutePath + "." + StubPredicateDefinition.NAME )
						.param( StubPredicateDefinition.IMPL_PARAM_NAME, (PredicateDefinition) context ->
								predicateWithRelativePath( context.predicate(), binding.flattened, 0 )
										.toPredicate() ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), dataSet.docId( 0 ) );
	}

	@Override
	protected final PredicateFinalStep predicate(SearchPredicateFactory f, ObjectFieldBinding objectFieldBinding,
			int matchingDocOrdinal) {
		return predicate( f, objectFieldBinding.absoluteFieldPath( dataSet.fieldType ), matchingDocOrdinal );
	}

	protected final PredicateFinalStep predicateWithRelativePath(SearchPredicateFactory f, ObjectFieldBinding objectFieldBinding,
			int matchingDocOrdinal) {
		return predicate( f, objectFieldBinding.relativeFieldPath( dataSet.fieldType ), matchingDocOrdinal );
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
