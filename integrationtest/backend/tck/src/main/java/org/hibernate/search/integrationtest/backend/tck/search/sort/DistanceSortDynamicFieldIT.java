/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.search.sort;

import static org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.AscendingUniqueDistanceFromCenterValues.CENTER_POINT;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;

import java.util.function.Function;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactory;
import org.hibernate.search.engine.search.sort.dsl.SortFinalStep;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.GeoPointFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.AscendingUniqueDistanceFromCenterValues;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class DistanceSortDynamicFieldIT {

	private static final GeoPointFieldTypeDescriptor fieldType = GeoPointFieldTypeDescriptor.INSTANCE;

	private static final String DOCUMENT_1 = "1";
	private static final String DOCUMENT_2 = "2";
	private static final String DOCUMENT_3 = "3";

	private static final String EMPTY = "empty";

	private static final int DOCUMENT_1_ORDINAL = 1;
	private static final int DOCUMENT_2_ORDINAL = 3;
	private static final int DOCUMENT_3_ORDINAL = 5;

	@RegisterExtension
	public static SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private static final SimpleMappedIndex<IndexBinding> mainIndex =
			SimpleMappedIndex.of( IndexBinding::new ).name( "main" );

	@BeforeAll
	static void setup() {
		setupHelper.start()
				.withIndexes( mainIndex )
				.setup();

		initData();
	}

	@Test
	void simple() {
		String fieldPath = mainFieldPath();

		assertThatQuery( matchNonEmptyQuery( f -> f.distance( fieldPath, CENTER_POINT ).asc() ) )
				.hasDocRefHitsExactOrder( mainIndex.typeName(), DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );
		assertThatQuery( matchNonEmptyQuery( f -> f.distance( fieldPath, CENTER_POINT ).desc() ) )
				.hasDocRefHitsExactOrder( mainIndex.typeName(), DOCUMENT_3, DOCUMENT_2, DOCUMENT_1 );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4531")
	void neverPopulated() {
		String neverPopulatedFieldPath = neverPopulatedFieldPath();
		String mainFieldPath = mainFieldPath();

		// The field that wasn't populated shouldn't have any effect on the sort,
		// but it shouldn't trigger an exception, either (see HSEARCH-4531).
		assertThatQuery( matchNonEmptyQuery( f -> f.composite()
				.add( f.distance( neverPopulatedFieldPath, CENTER_POINT ).asc() )
				.add( f.distance( mainFieldPath, CENTER_POINT ).asc() ) ) )
				.hasDocRefHitsExactOrder( mainIndex.typeName(), DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );
		assertThatQuery( matchNonEmptyQuery( f -> f.composite()
				.add( f.distance( neverPopulatedFieldPath, CENTER_POINT ).desc() )
				.add( f.distance( mainFieldPath, CENTER_POINT ).desc() ) ) )
				.hasDocRefHitsExactOrder( mainIndex.typeName(), DOCUMENT_3, DOCUMENT_2, DOCUMENT_1 );
	}

	private SearchQuery<DocumentReference> matchNonEmptyQuery(
			Function<? super SearchSortFactory, ? extends SortFinalStep> sortContributor) {
		return matchNonEmptyQuery( sortContributor, mainIndex.createScope() );
	}

	private SearchQuery<DocumentReference> matchNonEmptyQuery(
			Function<? super SearchSortFactory, ? extends SortFinalStep> sortContributor, StubMappingScope scope) {
		return scope.query()
				.where( f -> f.matchAll().except( f.id().matching( EMPTY ) ) )
				.sort( sortContributor )
				.toQuery();
	}

	private static String mainFieldPath() {
		return IndexBinding.fieldPath( "main" );
	}

	private static String neverPopulatedFieldPath() {
		return IndexBinding.fieldPath( "neverPopulated" );
	}

	private static void initData() {
		BulkIndexer mainIndexer = mainIndex.bulkIndexer()
				// Important: do not index the documents in the expected order after sorts (1, 2, 3)
				.add( DOCUMENT_2, document -> initDocument( document, DOCUMENT_2_ORDINAL ) )
				.add( EMPTY, document -> initDocument( document, null ) )
				.add( DOCUMENT_1, document -> initDocument( document, DOCUMENT_1_ORDINAL ) )
				.add( DOCUMENT_3, document -> initDocument( document, DOCUMENT_3_ORDINAL ) );
		mainIndexer.join();
	}

	private static void initDocument(DocumentElement document, Integer ordinal) {
		addValue( document, ordinal );
	}

	private static void addValue(DocumentElement documentElement, Integer ordinal) {
		if ( ordinal == null ) {
			return;
		}
		documentElement.addValue(
				mainFieldPath(),
				AscendingUniqueDistanceFromCenterValues.INSTANCE.getSingle().get( ordinal )
		);
	}

	private static class IndexBinding {
		public static String fieldPath(String suffix) {
			return "geoPoint_" + suffix;
		}

		IndexBinding(IndexSchemaElement root) {
			root.fieldTemplate( "myTemplate", f -> fieldType.configure( f ).sortable( Sortable.YES ) )
					.matchingPathGlob( fieldPath( "*" ) );
		}
	}

}
