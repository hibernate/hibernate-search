/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.aggregation;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.referenceProvider;

import java.util.Map;
import java.util.Optional;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Aggregable;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.aggregation.dsl.AggregationFinalStep;
import org.hibernate.search.engine.search.aggregation.dsl.SearchAggregationFactory;
import org.hibernate.search.engine.search.aggregation.dsl.SearchAggregationFactoryExtension;
import org.hibernate.search.engine.search.aggregation.dsl.spi.SearchAggregationDslContext;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingIndexManager;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;
import org.hibernate.search.util.impl.test.SubTest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.assertj.core.api.Assertions;

public class AggregationBaseIT {

	private static final String INDEX_NAME = "IndexName";

	private static final String DOCUMENT_1 = "doc1";
	private static final String DOCUMENT_2 = "doc2";
	private static final String DOCUMENT_3 = "doc3";
	private static final String EMPTY = "empty";

	private static final String STRING_1 = "Irving";
	private static final String STRING_2 = "Auster";

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	private IndexMapping indexMapping;
	private StubMappingIndexManager indexManager;

	@Before
	public void setup() {
		setupHelper.start()
				.withIndex(
						INDEX_NAME,
						ctx -> this.indexMapping = new IndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.indexManager = indexManager
				)
				.setup();

		initData();
	}

	@Test
	public void extension() {
		StubMappingScope scope = indexManager.createScope();
		SearchQuery<DocumentReference> query;
		AggregationKey<Map<String, Long>> aggregationKey = AggregationKey.of( "someAggregation" );

		// Mandatory extension, supported
		query = scope.query()
				.where( f -> f.matchAll() )
				.aggregation( aggregationKey, f -> f.extension( new SupportedExtension() )
						.extendedAggregation( "string" ) )
				.toQuery();
		SearchResultAssert.assertThat( query )
				.aggregation( aggregationKey )
				.satisfies( map -> Assertions.assertThat( map ).containsExactly(
						Assertions.entry( STRING_1, 2L ),
						Assertions.entry( STRING_2, 1L )
				) );

		// Mandatory extension, unsupported
		SubTest.expectException(
				() -> scope.aggregation().extension( new UnSupportedExtension() )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class );
	}

	private void initData() {
		IndexIndexingPlan<? extends DocumentElement> plan = indexManager.createIndexingPlan();
		plan.add( referenceProvider( DOCUMENT_1 ), document -> {
			document.addValue( indexMapping.string, STRING_1 );
		} );
		plan.add( referenceProvider( DOCUMENT_2 ), document -> {
			document.addValue( indexMapping.string, STRING_2 );
		} );
		plan.add( referenceProvider( DOCUMENT_3 ), document -> {
			document.addValue( indexMapping.string, STRING_1 );
		} );
		plan.add( referenceProvider( EMPTY ), document -> { } );

		plan.execute().join();

		// Check that all documents are searchable
		StubMappingScope scope = indexManager.createScope();
		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.matchAll() )
				.toQuery();
		assertThat( query ).hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3, EMPTY );
	}

	private static class IndexMapping {
		final IndexFieldReference<String> string;

		IndexMapping(IndexSchemaElement root) {
			string = root.field( "string", f -> f.asString().aggregable( Aggregable.YES ) )
					.toReference();
		}
	}

	private static class SupportedExtension implements SearchAggregationFactoryExtension<MyExtendedFactory> {
		@Override
		public Optional<MyExtendedFactory> extendOptional(SearchAggregationFactory original,
				SearchAggregationDslContext<?> dslContext) {
			Assertions.assertThat( original ).isNotNull();
			Assertions.assertThat( dslContext ).isNotNull();
			return Optional.of( new MyExtendedFactory( original ) );
		}
	}

	private static class UnSupportedExtension implements SearchAggregationFactoryExtension<MyExtendedFactory> {
		@Override
		public Optional<MyExtendedFactory> extendOptional(SearchAggregationFactory original,
				SearchAggregationDslContext<?> dslContext) {
			Assertions.assertThat( original ).isNotNull();
			Assertions.assertThat( dslContext ).isNotNull();
			return Optional.empty();
		}
	}

	private static class MyExtendedFactory {
		private final SearchAggregationFactory delegate;

		MyExtendedFactory(SearchAggregationFactory delegate) {
			this.delegate = delegate;
		}

		public AggregationFinalStep<Map<String, Long>> extendedAggregation(String absoluteFieldPath) {
			return delegate.terms().field( absoluteFieldPath, String.class );
		}
	}
}
