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

import org.hibernate.search.engine.backend.types.converter.runtime.spi.ToDocumentIdentifierValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.spi.ToDocumentIdentifierValueConverter;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingIndexManager;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;
import org.assertj.core.api.Assertions;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class MatchIdSearchPredicateIT {

	private static final String INDEX_NAME = "IndexName";
	private static final String COMPATIBLE_ID_CONVERTER_INDEX_NAME = "IndexWithCompatibleIdConverter";
	private static final String INCOMPATIBLE_ID_CONVERTER_INDEX_NAME = "IndexWithIncompatibleIdConverter";

	private static final String DOCUMENT_1 = "document1";
	private static final String DOCUMENT_2 = "document2";
	private static final String DOCUMENT_3 = "document3";
	private static final String COMPATIBLE_ID_CONVERTER_DOCUMENT_1 = "compatibleIdConverter_document1";
	private static final String INCOMPATIBLE_ID_CONVERTER_DOCUMENT_1 = "incompatibleIdConverter_document1";

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	private StubMappingIndexManager indexManager;
	private StubMappingIndexManager compatibleIdConverterIndexManager;
	private StubMappingIndexManager incompatibleIdConverterIndexManager;

	@Before
	public void setup() {
		setupHelper.start()
				.withIndex(
						INDEX_NAME,
						ctx -> { }, // Nothing to do, we don't need any field in the mapping
						indexManager -> this.indexManager = indexManager
				)
				.withIndex(
						COMPATIBLE_ID_CONVERTER_INDEX_NAME,
						ctx -> { }, // Nothing to do, we don't need any field in the mapping
						indexManager -> this.compatibleIdConverterIndexManager = indexManager
				)
				.withIndex(
						INCOMPATIBLE_ID_CONVERTER_INDEX_NAME,
						ctx -> ctx.idDslConverter( new IncompatibleIdConverter() ),
						indexManager -> this.incompatibleIdConverterIndexManager = indexManager
				)
				.setup();

		initData();
	}

	@Test
	public void matching() {
		StubMappingScope scope = indexManager.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.id().matching( DOCUMENT_1 ) )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );
	}

	@Test
	public void matching_then_matching() {
		StubMappingScope scope = indexManager.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.id()
						.matching( DOCUMENT_1 )
						.matching( DOCUMENT_3 )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_3 );
	}

	@Test
	public void matching_then_matchingAny() {
		StubMappingScope scope = indexManager.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.id()
						.matching( DOCUMENT_2 )
						.matchingAny( Arrays.asList( DOCUMENT_1 ) )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2 );
	}

	@Test
	public void matchingAny_singleElement() {
		StubMappingScope scope = indexManager.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.id()
						.matchingAny( Arrays.asList( DOCUMENT_1 ) )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );
	}

	@Test
	public void matchingAny_multipleElements() {
		StubMappingScope scope = indexManager.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.id()
						.matchingAny( Arrays.asList( DOCUMENT_1, DOCUMENT_3 ) )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_3 );
	}

	@Test
	public void multiIndex_withCompatibleIdConverterIndexManager_dslConverterEnabled() {
		StubMappingScope scope = indexManager.createScope( compatibleIdConverterIndexManager );

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.id().matching( DOCUMENT_1 ).matching( COMPATIBLE_ID_CONVERTER_DOCUMENT_1 ) )
				.toQuery();

		assertThat( query ).hasDocRefHitsAnyOrder( b -> {
			b.doc( INDEX_NAME, DOCUMENT_1 );
			b.doc( COMPATIBLE_ID_CONVERTER_INDEX_NAME, COMPATIBLE_ID_CONVERTER_DOCUMENT_1 );
		} );
	}

	@Test
	public void multiIndex_withIncompatibleIdConverterIndexManager_dslConverterEnabled() {
		StubMappingScope scope = indexManager.createScope( incompatibleIdConverterIndexManager );

		Assertions.assertThatThrownBy(
				() -> scope.predicate().id().matching( new Object() /* Value does not matter */ )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Multiple conflicting types for identifier" )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexNames( INDEX_NAME, INCOMPATIBLE_ID_CONVERTER_INDEX_NAME )
				) );
	}

	@Test
	public void multiIndex_withIncompatibleIdConverterIndexManager_dslConverterDisabled() {
		StubMappingScope scope = indexManager.createScope( incompatibleIdConverterIndexManager );

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.id().matching( DOCUMENT_1, ValueConvert.NO )
						.matching( INCOMPATIBLE_ID_CONVERTER_DOCUMENT_1, ValueConvert.NO ) )
				.toQuery();

		assertThat( query ).hasDocRefHitsAnyOrder( b -> {
			b.doc( INDEX_NAME, DOCUMENT_1 );
			b.doc( INCOMPATIBLE_ID_CONVERTER_INDEX_NAME, INCOMPATIBLE_ID_CONVERTER_DOCUMENT_1 );
		} );
	}

	private void initData() {
		IndexIndexingPlan<?> plan = indexManager.createIndexingPlan();
		plan.add( referenceProvider( DOCUMENT_1 ), document -> { } );
		plan.add( referenceProvider( DOCUMENT_2 ), document -> { } );
		plan.add( referenceProvider( DOCUMENT_3 ), document -> { } );
		plan.execute().join();

		plan = compatibleIdConverterIndexManager.createIndexingPlan();
		plan.add( referenceProvider( COMPATIBLE_ID_CONVERTER_DOCUMENT_1 ), document -> { } );
		plan.execute().join();

		plan = incompatibleIdConverterIndexManager.createIndexingPlan();
		plan.add( referenceProvider( INCOMPATIBLE_ID_CONVERTER_DOCUMENT_1 ), document -> { } );
		plan.execute().join();

		// Check that all documents are searchable
		assertThat(
				indexManager.createScope().query()
						.where( f -> f.matchAll() )
						.toQuery()
		)
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );
		assertThat(
				compatibleIdConverterIndexManager.createScope().query()
						.where( f -> f.matchAll() )
						.toQuery()
		)
				.hasDocRefHitsAnyOrder( COMPATIBLE_ID_CONVERTER_INDEX_NAME, COMPATIBLE_ID_CONVERTER_DOCUMENT_1 );
		assertThat(
				incompatibleIdConverterIndexManager.createScope().query()
						.where( f -> f.matchAll() )
						.toQuery()
		)
				.hasDocRefHitsAnyOrder( INCOMPATIBLE_ID_CONVERTER_INDEX_NAME, INCOMPATIBLE_ID_CONVERTER_DOCUMENT_1 );
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
