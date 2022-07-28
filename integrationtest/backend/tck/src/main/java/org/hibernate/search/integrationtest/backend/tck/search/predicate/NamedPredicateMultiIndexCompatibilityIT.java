/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;

import java.util.Objects;

import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.definition.PredicateDefinition;
import org.hibernate.search.engine.search.predicate.definition.PredicateDefinitionContext;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.KeywordStringFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModel;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

public class NamedPredicateMultiIndexCompatibilityIT {

	@ClassRule
	public static final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private static final SimpleMappedIndex<IndexBinding> index =
			SimpleMappedIndex.of( IndexBinding::new ).name( "main" );
	private static final SimpleMappedIndex<IndexBinding> compatibleIndex =
			SimpleMappedIndex.of( IndexBinding::new ).name( "compatible" );
	private static final SimpleMappedIndex<MissingPredicateIndexBinding> missingPredicateIndex =
			SimpleMappedIndex.of( MissingPredicateIndexBinding::new ).name( "missingPredicate" );
	private static final SimpleMappedIndex<IncompatibleIndexBinding> incompatibleIndex =
			SimpleMappedIndex.of( IncompatibleIndexBinding::new ).name( "incompatible" );
	protected static final DataSet dataSet = new DataSet();

	@BeforeClass
	public static void setup() {
		setupHelper.start()
				.withIndexes( index, compatibleIndex, missingPredicateIndex, incompatibleIndex )
				.setup();

		BulkIndexer mainIndexer = index.bulkIndexer();
		BulkIndexer compatibleIndexer = compatibleIndex.bulkIndexer();
		dataSet.contribute( mainIndexer, compatibleIndexer );
		mainIndexer.join( compatibleIndexer );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4166")
	public void multiIndex_withCompatibleIndex() {
		StubMappingScope scope = index.createScope( compatibleIndex );

		assertThatQuery( scope.query()
				.where( f -> f.named( "my-predicate" ).param( "value", "main" ) ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 0 ) );
		assertThatQuery( scope.query()
				.where( f -> f.named( "my-predicate" ).param( "value", "compatible" ) ) )
				.hasDocRefHitsAnyOrder( compatibleIndex.typeName(), dataSet.docId( 1 ) );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4166")
	public void multiIndex_withMissingPredicateIndex() {
		StubMappingScope scope = index.createScope( missingPredicateIndex );

		assertThatThrownBy( () -> scope.predicate().named( "my-predicate" ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Inconsistent configuration for index schema root in a search query across multiple indexes",
						"Inconsistent support for 'predicate:named:my-predicate'",
						"'predicate:named:my-predicate' can be used in some of the targeted indexes, but not all of them"
				)
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexNames( index.name(), missingPredicateIndex.name() )
				) );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4166")
	public void multiIndex_withIncompatibleIndex() {
		StubMappingScope scope = index.createScope( incompatibleIndex );

		assertThatThrownBy( () -> scope.predicate().named( "my-predicate" ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Inconsistent configuration for index schema root in a search query across multiple indexes",
						"Inconsistent support for 'predicate:named:my-predicate'",
						"Predicate definition differs:", TestedPredicateDefinition.class.getName()
				)
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexNames( index.name(), incompatibleIndex.name() )
				) );
	}

	public static final class IndexBinding {
		private final SimpleFieldModel<String> field;

		public IndexBinding(IndexSchemaElement root) {
			field = SimpleFieldModel.mapper( KeywordStringFieldTypeDescriptor.INSTANCE )
					.map( root, "field" );
			root.namedPredicate( "my-predicate", new TestedPredicateDefinition( "field" ) );
		}
	}

	public static class MissingPredicateIndexBinding {
		private final SimpleFieldModel<String> field;
		public MissingPredicateIndexBinding(IndexSchemaElement root) {
			field = SimpleFieldModel.mapper( KeywordStringFieldTypeDescriptor.INSTANCE )
					.map( root, "field" );
		}
	}

	public static final class IncompatibleIndexBinding {
		private final SimpleFieldModel<String> field2;

		public IncompatibleIndexBinding(IndexSchemaElement root) {
			field2 = SimpleFieldModel.mapper( KeywordStringFieldTypeDescriptor.INSTANCE )
					.map( root, "field2" );
			root.namedPredicate( "my-predicate", new TestedPredicateDefinition( "field2" ) );
		}
	}

	public static final class DataSet
			extends AbstractPredicateDataSet {
		public DataSet() {
			super( null );
		}

		public void contribute(BulkIndexer mainIndexer, BulkIndexer compatibleIndexer) {
			mainIndexer.add( docId( 0 ), routingKey,
					document -> document.addValue( index.binding().field.reference, "main" ) );
			compatibleIndexer.add( docId( 1 ), routingKey,
					document -> document.addValue( compatibleIndex.binding().field.reference, "compatible" ) );
		}
	}

	private static class TestedPredicateDefinition implements PredicateDefinition {
		private final String fieldName;
		public TestedPredicateDefinition(
				String fieldName) {
			this.fieldName = fieldName;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			TestedPredicateDefinition that = (TestedPredicateDefinition) o;
			return Objects.equals( fieldName, that.fieldName );
		}

		@Override
		public int hashCode() {
			return Objects.hash( fieldName );
		}

		@Override
		public SearchPredicate create(PredicateDefinitionContext context) {
			return context.predicate().match().field( fieldName )
					.matching( context.param( "value" ) )
					.toPredicate();
		}
	}
}
