/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.lucene.search;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMapperUtils.referenceProvider;

import java.util.function.Function;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkPlan;
import org.hibernate.search.engine.search.sort.dsl.SortFinalStep;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingIndexManager;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingScope;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactory;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.common.SearchException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class LuceneSearchSortIT {

	private static final String INDEX_NAME = "IndexName";

	private static final String FIRST_ID = "1";
	private static final String SECOND_ID = "2";
	private static final String THIRD_ID = "3";
	private static final String EMPTY_ID = "empty";

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Rule
	public ExpectedException thrown = ExpectedException.none();

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

	private SearchQuery<DocumentReference> simpleQuery(
			Function<? super SearchSortFactory, ? extends SortFinalStep> sortContributor) {
		StubMappingScope scope = indexManager.createScope();
		return scope.query()
				.predicate( f -> f.matchAll() )
				.sort( sortContributor )
				.toQuery();
	}

	@Test
	public void byDistanceDesc() {
		thrown.expect( SearchException.class );
		thrown.expectMessage( "HSEARCH600043" );

		simpleQuery( b -> b.distance( "geoPoint", GeoPoint.of( 45.757864, 4.834496 ) ).desc() );
	}

	private void initData() {
		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan();
		workPlan.add( referenceProvider( FIRST_ID ), document -> {
			document.addValue( indexMapping.geoPoint, GeoPoint.of( 45.7705687,4.835233 ) );
		} );
		workPlan.add( referenceProvider( SECOND_ID ), document -> {
			document.addValue( indexMapping.geoPoint, GeoPoint.of( 45.7541719, 4.8386221 ) );
		} );
		workPlan.add( referenceProvider( THIRD_ID ), document -> {
			document.addValue( indexMapping.geoPoint, GeoPoint.of( 45.7530374, 4.8510299 ) );
		} );
		workPlan.add( referenceProvider( EMPTY_ID ), document -> { } );

		workPlan.execute().join();

		// Check that all documents are searchable
		StubMappingScope scope = indexManager.createScope();
		SearchQuery<DocumentReference> query = scope.query()
				.predicate( f -> f.matchAll() )
				.toQuery();
		assertThat( query ).hasDocRefHitsAnyOrder( INDEX_NAME, FIRST_ID, SECOND_ID, THIRD_ID, EMPTY_ID );
	}

	private static class IndexMapping {
		final IndexFieldReference<GeoPoint> geoPoint;

		IndexMapping(IndexSchemaElement root) {
			geoPoint = root.field(
					"geoPoint",
					f -> f.asGeoPoint().sortable( Sortable.YES )
			)
					.toReference();
		}
	}
}
