/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.aggregation;

import java.util.Map;
import java.util.Optional;

import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Aggregable;
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
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.assertj.core.api.Assertions;

public class AggregationBaseIT {

	private static final String DOCUMENT_1 = "doc1";
	private static final String DOCUMENT_2 = "doc2";
	private static final String DOCUMENT_3 = "doc3";
	private static final String EMPTY = "empty";

	private static final String STRING_1 = "Irving";
	private static final String STRING_2 = "Auster";

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	@Before
	public void setup() {
		setupHelper.start().withIndex( index ).setup();

		initData();
	}

	@Test
	public void extension() {
		StubMappingScope scope = index.createScope();
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
		Assertions.assertThatThrownBy(
				() -> scope.aggregation().extension( new UnSupportedExtension() )
		)
				.isInstanceOf( SearchException.class );
	}

	private void initData() {
		index.bulkIndexer()
				.add( DOCUMENT_1, document -> {
					document.addValue( index.binding().string, STRING_1 );
				} )
				.add( DOCUMENT_2, document -> {
					document.addValue( index.binding().string, STRING_2 );
				} )
				.add( DOCUMENT_3, document -> {
					document.addValue( index.binding().string, STRING_1 );
				} )
				.add( EMPTY, document -> { } )
				.join();
	}

	private static class IndexBinding {
		final IndexFieldReference<String> string;

		IndexBinding(IndexSchemaElement root) {
			string = root.field( "string", f -> f.asString().aggregable( Aggregable.YES ) )
					.toReference();
		}
	}

	private static class SupportedExtension implements SearchAggregationFactoryExtension<MyExtendedFactory> {
		@Override
		public Optional<MyExtendedFactory> extendOptional(SearchAggregationFactory original,
				SearchAggregationDslContext<?, ?> dslContext) {
			Assertions.assertThat( original ).isNotNull();
			Assertions.assertThat( dslContext ).isNotNull();
			return Optional.of( new MyExtendedFactory( original ) );
		}
	}

	private static class UnSupportedExtension implements SearchAggregationFactoryExtension<MyExtendedFactory> {
		@Override
		public Optional<MyExtendedFactory> extendOptional(SearchAggregationFactory original,
				SearchAggregationDslContext<?, ?> dslContext) {
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
