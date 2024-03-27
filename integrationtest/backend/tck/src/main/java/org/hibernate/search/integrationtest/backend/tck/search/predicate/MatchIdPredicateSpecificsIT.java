/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hibernate.search.engine.backend.types.converter.ToDocumentValueConverter;
import org.hibernate.search.engine.backend.types.converter.runtime.ToDocumentValueConvertContext;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class MatchIdPredicateSpecificsIT {

	// it should be more than the default max clause count of the boolean queries
	private static final int LOT_OF_TERMS_SIZE = 2000;

	private static final String DOCUMENT_1 = "document1";
	private static final String DOCUMENT_2 = "document2";
	private static final String DOCUMENT_3 = "document3";
	private static final String COMPATIBLE_ID_CONVERTER_DOCUMENT_1 = "compatibleIdConverter_document1";
	private static final String INCOMPATIBLE_ID_CONVERTER_DOCUMENT_1 = "incompatibleIdConverter_document1";

	@RegisterExtension
	public static final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private static final StubMappedIndex mainIndex =
			StubMappedIndex.withoutFields().name( "main" );
	private static final StubMappedIndex compatibleIdConverterIndex =
			StubMappedIndex.withoutFields().name( "compatibleIdConverter" );
	private static final StubMappedIndex incompatibleIdConverterIndex =
			StubMappedIndex.ofAdvancedNonRetrievable( ctx -> ctx
					.idDslConverter( Object.class, new IncompatibleIdConverter() ) )
					.name( "incompatibleIdConverter" );

	// only one of which is matching
	private static final List<String> lotsOfTerms = new ArrayList<>();

	@BeforeAll
	static void setup() {
		setupHelper.start().withIndexes( mainIndex, compatibleIdConverterIndex, incompatibleIdConverterIndex ).setup();

		for ( int i = 1; i <= LOT_OF_TERMS_SIZE; i++ ) {
			if ( i == 3 ) {
				lotsOfTerms.add( "document" + i );
			}
			lotsOfTerms.add( "wrong-document" + i );
		}

		initData();
	}

	@Test
	void matching() {
		assertThatQuery( mainIndex.query()
				.where( f -> f.id().matching( DOCUMENT_1 ) ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1 );
	}

	@Test
	void matching_then_matching() {
		assertThatQuery( mainIndex.query()
				.where( f -> f.id()
						.matching( DOCUMENT_1 )
						.matching( DOCUMENT_3 ) ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1, DOCUMENT_3 );
	}

	@Test
	void matching_then_matchingAny() {
		assertThatQuery( mainIndex.query()
				.where( f -> f.id()
						.matching( DOCUMENT_2 )
						.matchingAny( Arrays.asList( DOCUMENT_1 ) ) ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1, DOCUMENT_2 );
	}

	@Test
	void matchingAny_singleElement() {
		assertThatQuery( mainIndex.query()
				.where( f -> f.id()
						.matchingAny( Arrays.asList( DOCUMENT_1 ) ) ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1 );
	}

	@Test
	void matchingAny_multipleElements() {
		assertThatQuery( mainIndex.query()
				.where( f -> f.id()
						.matchingAny( Arrays.asList( DOCUMENT_1, DOCUMENT_3 ) ) ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1, DOCUMENT_3 );
	}

	@Test
	void matchingAny_lotsOfTerms() {
		assertThatQuery( mainIndex.query()
				.where( f -> f.id()
						.matchingAny( lotsOfTerms ) ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_3 );
	}

	@Test
	void multiIndex_withCompatibleIdConverterIndexManager_dslConverterEnabled() {
		StubMappingScope scope = mainIndex.createScope( compatibleIdConverterIndex );

		assertThatQuery( scope.query()
				.where( f -> f.id().matching( DOCUMENT_1 ).matching( COMPATIBLE_ID_CONVERTER_DOCUMENT_1 ) ) )
				.hasDocRefHitsAnyOrder( b -> {
					b.doc( mainIndex.typeName(), DOCUMENT_1 );
					b.doc( compatibleIdConverterIndex.typeName(), COMPATIBLE_ID_CONVERTER_DOCUMENT_1 );
				} );
	}

	@Test
	void multiIndex_withIncompatibleIdConverterIndex_dslConverterEnabled() {
		SearchPredicateFactory f = mainIndex.createScope( incompatibleIdConverterIndex ).predicate();

		assertThatThrownBy( () -> f.id().matching( new Object() /* Value does not matter */ ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Inconsistent configuration for identifier in a search query across multiple indexes",
						"Attribute 'dslConverter' differs:", " vs. "
				)
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexNames( mainIndex.name(), incompatibleIdConverterIndex.name() )
				) );
	}

	@Test
	void multiIndex_withIncompatibleIdConverterIndex_dslConverterDisabled() {
		StubMappingScope scope = mainIndex.createScope( incompatibleIdConverterIndex );

		assertThatQuery( scope.query()
				.where( f -> f.id().matching( DOCUMENT_1, ValueConvert.NO )
						.matching( INCOMPATIBLE_ID_CONVERTER_DOCUMENT_1, ValueConvert.NO ) ) )
				.hasDocRefHitsAnyOrder( b -> {
					b.doc( mainIndex.typeName(), DOCUMENT_1 );
					b.doc( incompatibleIdConverterIndex.typeName(), INCOMPATIBLE_ID_CONVERTER_DOCUMENT_1 );
				} );
	}

	private static void initData() {
		BulkIndexer mainIndexer = mainIndex.bulkIndexer()
				.add( DOCUMENT_1, document -> {} )
				.add( DOCUMENT_2, document -> {} )
				.add( DOCUMENT_3, document -> {} );
		BulkIndexer compatibleIdConverterIndexer = compatibleIdConverterIndex.bulkIndexer()
				.add( COMPATIBLE_ID_CONVERTER_DOCUMENT_1, document -> {} );
		BulkIndexer incompatibleIdConverterIndexer = incompatibleIdConverterIndex.bulkIndexer()
				.add( INCOMPATIBLE_ID_CONVERTER_DOCUMENT_1, document -> {} );
		mainIndexer.join( compatibleIdConverterIndexer, incompatibleIdConverterIndexer );
	}

	private static class IncompatibleIdConverter implements ToDocumentValueConverter<Object, String> {
		@Override
		public String toDocumentValue(Object value, ToDocumentValueConvertContext context) {
			throw new UnsupportedOperationException( "Should not be called" );
		}
	}
}
