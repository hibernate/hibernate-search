/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.spatial;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.referenceProvider;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.spatial.GeoBoundingBox;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingIndexManager;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

@TestForIssue(jiraKey = "HSEARCH-2859")
public class MultivaluedSpatialIT {

	private static final String INDEX_NAME = "IndexName";

	// latitude increases from south to north
	// longitude increases from west to east
	private static final GeoPoint NORTH_WEST = GeoPoint.of( 7.0, 3.0 );
	private static final GeoPoint NORTH_EAST = GeoPoint.of( 7.0, 7.0 );
	private static final GeoPoint SOUTH_WEST = GeoPoint.of( 3.0, 3.0 );
	private static final GeoPoint SOUTH_EAST = GeoPoint.of( 3.0, 7.0 );

	private static final GeoBoundingBox AROUND_NORTH_WEST_BOX =
			GeoBoundingBox.of( GeoPoint.of( 8.0, 2.0 ), GeoPoint.of( 6.0, 4.0 ) );

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	protected IndexMapping indexMapping;
	protected StubMappingIndexManager indexManager;

	@Before
	public void setup() {
		setupHelper.start()
				.withIndex(
						INDEX_NAME,
						ctx -> this.indexMapping = new IndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.indexManager = indexManager
				).setup();

		initData();
	}

	@Test
	public void boundingBox() {
		StubMappingScope scope = indexManager.createScope();
		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.spatial().within().field( "geoPoint" ).boundingBox( AROUND_NORTH_WEST_BOX ) )
				.toQuery();

		assertThat( query ).hasDocRefHitsAnyOrder( INDEX_NAME, "1" );
	}

	private void initData() {
		IndexIndexingPlan<?> plan = indexManager.createIndexingPlan();
		plan.add( referenceProvider( "1" ), f -> {
			f.addValue( indexMapping.geoPoint, NORTH_WEST );
			f.addValue( indexMapping.geoPoint, SOUTH_EAST );
		} );
		plan.add( referenceProvider( "2" ), f -> {
			f.addValue( indexMapping.geoPoint, NORTH_EAST );
			f.addValue( indexMapping.geoPoint, SOUTH_WEST );
		} );
		plan.execute().join();
	}

	protected static class IndexMapping {
		final IndexFieldReference<GeoPoint> geoPoint;

		IndexMapping(IndexSchemaElement root) {
			geoPoint = root.field( "geoPoint", f -> f.asGeoPoint() )
					.multiValued().toReference();
		}
	}
}
