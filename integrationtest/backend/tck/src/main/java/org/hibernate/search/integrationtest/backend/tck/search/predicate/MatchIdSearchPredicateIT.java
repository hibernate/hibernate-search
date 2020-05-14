/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.referenceProvider;

import java.util.Arrays;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.types.converter.runtime.spi.ToDocumentIdentifierValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.spi.ToDocumentIdentifierValueConverter;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.assertj.core.api.Assertions;

public class MatchIdSearchPredicateIT {

	private static final String DOCUMENT_1 = "document1";
	private static final String DOCUMENT_2 = "document2";
	private static final String DOCUMENT_3 = "document3";
	private static final String COMPATIBLE_ID_CONVERTER_DOCUMENT_1 = "compatibleIdConverter_document1";
	private static final String INCOMPATIBLE_ID_CONVERTER_DOCUMENT_1 = "incompatibleIdConverter_document1";

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private final StubMappedIndex mainIndex =
			StubMappedIndex.withoutFields().name( "main" );
	private final StubMappedIndex compatibleIdConverterIndex =
			StubMappedIndex.withoutFields().name( "compatibleIdConverter" );
	private final StubMappedIndex incompatibleIdConverterIndex =
			StubMappedIndex.ofAdvancedNonRetrievable( ctx -> ctx.idDslConverter( new IncompatibleIdConverter() ) )
					.name( "incompatibleIdConverter" );

	@Before
	public void setup() {
		setupHelper.start().withIndexes( mainIndex, compatibleIdConverterIndex, incompatibleIdConverterIndex ).setup();

		initData();
	}

	@Test
	public void matching() {
		StubMappingScope scope = mainIndex.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.id().matching( DOCUMENT_1 ) )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1 );
	}

	@Test
	public void matching_then_matching() {
		StubMappingScope scope = mainIndex.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.id()
						.matching( DOCUMENT_1 )
						.matching( DOCUMENT_3 )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1, DOCUMENT_3 );
	}

	@Test
	public void matching_then_matchingAny() {
		StubMappingScope scope = mainIndex.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.id()
						.matching( DOCUMENT_2 )
						.matchingAny( Arrays.asList( DOCUMENT_1 ) )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1, DOCUMENT_2 );
	}

	@Test
	public void matchingAny_singleElement() {
		StubMappingScope scope = mainIndex.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.id()
						.matchingAny( Arrays.asList( DOCUMENT_1 ) )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1 );
	}

	@Test
	public void matchingAny_multipleElements() {
		StubMappingScope scope = mainIndex.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.id()
						.matchingAny( Arrays.asList( DOCUMENT_1, DOCUMENT_3 ) )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1, DOCUMENT_3 );
	}

	@Test
	public void multiIndex_withCompatibleIdConverterIndexManager_dslConverterEnabled() {
		StubMappingScope scope = mainIndex.createScope( compatibleIdConverterIndex );

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.id().matching( DOCUMENT_1 ).matching( COMPATIBLE_ID_CONVERTER_DOCUMENT_1 ) )
				.toQuery();

		assertThat( query ).hasDocRefHitsAnyOrder( b -> {
			b.doc( mainIndex.typeName(), DOCUMENT_1 );
			b.doc( compatibleIdConverterIndex.typeName(), COMPATIBLE_ID_CONVERTER_DOCUMENT_1 );
		} );
	}

	@Test
	public void multiIndex_withIncompatibleIdConverterIndex_dslConverterEnabled() {
		StubMappingScope scope = mainIndex.createScope( incompatibleIdConverterIndex );

		Assertions.assertThatThrownBy(
				() -> scope.predicate().id().matching( new Object() /* Value does not matter */ )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Multiple conflicting types for identifier" )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexNames( mainIndex.name(), incompatibleIdConverterIndex.name() )
				) );
	}

	@Test
	public void multiIndex_withIncompatibleIdConverterIndex_dslConverterDisabled() {
		StubMappingScope scope = mainIndex.createScope( incompatibleIdConverterIndex );

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.id().matching( DOCUMENT_1, ValueConvert.NO )
						.matching( INCOMPATIBLE_ID_CONVERTER_DOCUMENT_1, ValueConvert.NO ) )
				.toQuery();

		assertThat( query ).hasDocRefHitsAnyOrder( b -> {
			b.doc( mainIndex.typeName(), DOCUMENT_1 );
			b.doc( incompatibleIdConverterIndex.typeName(), INCOMPATIBLE_ID_CONVERTER_DOCUMENT_1 );
		} );
	}

	private void initData() {
		IndexIndexingPlan<?> plan = mainIndex.createIndexingPlan();
		plan.add( referenceProvider( DOCUMENT_1 ), document -> { } );
		plan.add( referenceProvider( DOCUMENT_2 ), document -> { } );
		plan.add( referenceProvider( DOCUMENT_3 ), document -> { } );
		plan.execute().join();

		plan = compatibleIdConverterIndex.createIndexingPlan();
		plan.add( referenceProvider( COMPATIBLE_ID_CONVERTER_DOCUMENT_1 ), document -> { } );
		plan.execute().join();

		plan = incompatibleIdConverterIndex.createIndexingPlan();
		plan.add( referenceProvider( INCOMPATIBLE_ID_CONVERTER_DOCUMENT_1 ), document -> { } );
		plan.execute().join();

		// Check that all documents are searchable
		assertThat(
				mainIndex.createScope().query()
						.where( f -> f.matchAll() )
						.toQuery()
		)
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );
		assertThat(
				compatibleIdConverterIndex.createScope().query()
						.where( f -> f.matchAll() )
						.toQuery()
		)
				.hasDocRefHitsAnyOrder( compatibleIdConverterIndex.typeName(), COMPATIBLE_ID_CONVERTER_DOCUMENT_1 );
		assertThat(
				incompatibleIdConverterIndex.createScope().query()
						.where( f -> f.matchAll() )
						.toQuery()
		)
				.hasDocRefHitsAnyOrder( incompatibleIdConverterIndex.typeName(), INCOMPATIBLE_ID_CONVERTER_DOCUMENT_1 );
	}

	private static class IncompatibleIdConverter implements ToDocumentIdentifierValueConverter<String> {
		@Override
		public String convert(String value, ToDocumentIdentifierValueConvertContext context) {
			throw new UnsupportedOperationException( "Should not be called" );
		}

		@Override
		public String convertUnknown(Object value, ToDocumentIdentifierValueConvertContext context) {
			throw new UnsupportedOperationException( "Should not be called" );
		}
	}
}
