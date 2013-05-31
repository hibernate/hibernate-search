/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */

package org.hibernate.search.test.spatial;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Transient;

import junit.framework.Assert;

import org.apache.lucene.search.Sort;

import org.hibernate.Session;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Latitude;
import org.hibernate.search.annotations.Longitude;
import org.hibernate.search.annotations.Spatial;
import org.hibernate.search.annotations.SpatialMode;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.query.dsl.Unit;
import org.hibernate.search.spatial.DistanceSortField;
import org.hibernate.search.test.SearchTestCase;
import org.hibernate.testing.TestForIssue;

/**
 * Spatial search with sort by distance and paging orders entities
 *
 * @author PB
 */
@TestForIssue(jiraKey = "HSEARCH-1267")
public class SpatialSearchSortByDistanceAndPaging extends SearchTestCase {

	private static final int EXPECTED_RESULTS_COUNT = 37;
	private static final double CENTER_LAT = 54.0;
	private static final double CENTER_LON = 18.0;
	private static final double SEARCH_DISTANCE = 20.0;
	private Map<Long, Integer> entitiesIdsSet;
	private int idx;

	public void testSortWithoutPaging_isOk() {
		Assert.assertEquals( EXPECTED_RESULTS_COUNT, doSearch( SEARCH_DISTANCE, 50, true ) );
	}

	public void testSortWithPageSize5_notOk() {
		Assert.assertEquals(
				"sorting by distance and paging error",
				EXPECTED_RESULTS_COUNT, doSearch( SEARCH_DISTANCE, 5, true )
		);
	}

	public void testSortWithPageSize10_notOk() {
		Assert.assertEquals(
				"sorting by distance and paging error",
				EXPECTED_RESULTS_COUNT, doSearch( SEARCH_DISTANCE, 10, true )
		);
	}

	public void testNoSortWithPageSize5_isOk() {
		Assert.assertEquals( EXPECTED_RESULTS_COUNT, doSearch( SEARCH_DISTANCE, 5, false ) );
	}

	public void testNoSortWithPageSize10_isOk() {
		Assert.assertEquals( EXPECTED_RESULTS_COUNT, doSearch( SEARCH_DISTANCE, 10, false ) );
	}

	@Override
	public void setUp() throws Exception {
		super.setUp();
		prepareTestData();
	}

	/**
	 * Search and iterate result pages.
	 *
	 * @return unique result count
	 */
	public int doSearch(double distance, int pageSize, boolean sortByDistance) {
		System.out.println(
				String.format(
						"distance %.2f pageSize %d, sortByDistance %s",
						distance, pageSize, sortByDistance
				)
		);

		entitiesIdsSet = new HashMap<Long, Integer>();
		idx = 0;
		int firstResult = 0;
		List result;

		while ( (result = distanceSearch( CENTER_LAT, CENTER_LON, distance, firstResult, pageSize, sortByDistance ))
				!= null && !result.isEmpty() ) {

			printResults( result );

			firstResult += pageSize;
		}
		return entitiesIdsSet.size();
	}

	/**
	 * Show result.
	 */
	private void printResults(List<GeoEntity> list) {
		for ( GeoEntity entity : list ) {
			System.out.println(
					String.format(
							"%d %f %d%s",
							idx, entity.getDistance(), entity.getId(),
							entitiesIdsSet.containsKey( entity.getId() )
									? " was at index " + entitiesIdsSet.get( entity.getId() ) : ""
					)
			);
			if ( !entitiesIdsSet.containsKey( entity.getId() ) ) {
				entitiesIdsSet.put( entity.getId(), idx );
			}
			idx++;
		}
		System.out.println();
	}

	/**
	 * Search GeoEntities starting from startLat and startLon within distance
	 */
	private List distanceSearch(
			double startLat,
			double startLon,
			double distance,
			int firstResult,
			int maxResult,
			boolean sortByDistance) {

		List resultList = new ArrayList();

		Session sessionHbn = openSession();
		sessionHbn.beginTransaction();

		FullTextSession fTxtSess = Search.getFullTextSession( sessionHbn );

		QueryBuilder builder = fTxtSess.getSearchFactory().buildQueryBuilder().forEntity( GeoEntity.class ).get();

		org.apache.lucene.search.Query luceneQuery = builder.spatial()
				.onDefaultCoordinates()
				.within( distance, Unit.KM )
				.ofLatitude( startLat )
				.andLongitude( startLon )
				.createQuery();

		FullTextQuery hibQuery = fTxtSess.createFullTextQuery( luceneQuery, GeoEntity.class );
		hibQuery.setProjection( FullTextQuery.THIS, FullTextQuery.SPATIAL_DISTANCE );
		hibQuery.setSpatialParameters( startLat, startLon, Spatial.COORDINATES_DEFAULT_FIELD );

		if ( sortByDistance ) {
			Sort distanceSort = new Sort(
					new DistanceSortField(
							startLat,
							startLon,
							Spatial.COORDINATES_DEFAULT_FIELD
					)
			);
			hibQuery.setSort( distanceSort );
		}

		hibQuery.setFirstResult( firstResult ).setMaxResults( maxResult );

		List tmpList = hibQuery.list();

		// copy distance from projection to entities
		for ( Object obj[] : (List<Object[]>) tmpList ) {
			GeoEntity entity = (GeoEntity) obj[0];
			entity.setDistance( (Double) obj[1] );
			resultList.add( entity );
		}

		sessionHbn.getTransaction().commit();
		sessionHbn.close();
		return resultList;
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				GeoEntity.class
		};
	}

	@Entity
	@Indexed
	@Spatial(spatialMode = SpatialMode.RANGE)
	public static class GeoEntity {

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		@DocumentId
		private Long id;
		@Latitude
		private Double lat;
		@Longitude
		private Double lon;
		@Transient
		private Double distance;
		private String value;

		public GeoEntity() {
		}

		public GeoEntity(Double lat, Double lon, String value) {
			super();
			this.value = value;
			this.lat = lat;
			this.lon = lon;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Double getLat() {
			return lat;
		}

		public void setLat(Double lat) {
			this.lat = lat;
		}

		public Double getLon() {
			return lon;
		}

		public void setLon(Double lon) {
			this.lon = lon;
		}

		public Double getDistance() {
			return distance;
		}

		public void setDistance(Double distance) {
			this.distance = distance;
		}

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}
	}

	private void prepareTestData() {
		Session sessionHbn = openSession();
		sessionHbn.beginTransaction();

		sessionHbn.saveOrUpdate( new GeoEntity( 54.021861, 18.048349, "v00" ) );
		sessionHbn.saveOrUpdate( new GeoEntity( 54.079361, 18.185003, "v01" ) );
		sessionHbn.saveOrUpdate( new GeoEntity( 54.197314, 18.158194, "v02" ) );
		sessionHbn.saveOrUpdate( new GeoEntity( 54.177621, 18.150250, "v03" ) );
		sessionHbn.saveOrUpdate( new GeoEntity( 54.125537, 18.075425, "v04" ) );
		sessionHbn.saveOrUpdate( new GeoEntity( 54.070479, 18.064328, "v05" ) );
		sessionHbn.saveOrUpdate( new GeoEntity( 54.169191, 18.025334, "v06" ) );
		sessionHbn.saveOrUpdate( new GeoEntity( 54.031556, 18.059142, "v07" ) );
		sessionHbn.saveOrUpdate( new GeoEntity( 54.068104, 18.064764, "v08" ) );
		sessionHbn.saveOrUpdate( new GeoEntity( 54.103417, 18.182243, "v09" ) );
		sessionHbn.saveOrUpdate( new GeoEntity( 54.002537, 18.037648, "v10" ) );
		sessionHbn.saveOrUpdate( new GeoEntity( 54.223375, 18.003011, "v11" ) );
		sessionHbn.saveOrUpdate( new GeoEntity( 54.061524, 18.071648, "v12" ) );
		sessionHbn.saveOrUpdate( new GeoEntity( 54.001563, 18.055457, "v13" ) );
		sessionHbn.saveOrUpdate( new GeoEntity( 54.032163, 18.138202, "v14" ) );
		sessionHbn.saveOrUpdate( new GeoEntity( 54.229162, 18.109120, "v15" ) );
		sessionHbn.saveOrUpdate( new GeoEntity( 54.089290, 18.146594, "v16" ) );
		sessionHbn.saveOrUpdate( new GeoEntity( 54.066932, 18.227589, "v17" ) );
		sessionHbn.saveOrUpdate( new GeoEntity( 54.231832, 18.186478, "v18" ) );
		sessionHbn.saveOrUpdate( new GeoEntity( 54.088117, 18.206227, "v19" ) );
		sessionHbn.saveOrUpdate( new GeoEntity( 54.055539, 18.216111, "v20" ) );
		sessionHbn.saveOrUpdate( new GeoEntity( 54.175193, 18.026838, "v21" ) );
		sessionHbn.saveOrUpdate( new GeoEntity( 54.171762, 18.215527, "v22" ) );
		sessionHbn.saveOrUpdate( new GeoEntity( 54.196967, 18.177212, "v23" ) );
		sessionHbn.saveOrUpdate( new GeoEntity( 54.028481, 18.084819, "v24" ) );
		sessionHbn.saveOrUpdate( new GeoEntity( 54.171071, 18.057752, "v25" ) );
		sessionHbn.saveOrUpdate( new GeoEntity( 54.062881, 18.117777, "v26" ) );
		sessionHbn.saveOrUpdate( new GeoEntity( 54.247034, 18.235728, "v27" ) );
		sessionHbn.saveOrUpdate( new GeoEntity( 54.014431, 18.220235, "v28" ) );
		sessionHbn.saveOrUpdate( new GeoEntity( 54.076813, 18.010077, "v29" ) );
		sessionHbn.saveOrUpdate( new GeoEntity( 54.103647, 18.170640, "v30" ) );
		sessionHbn.saveOrUpdate( new GeoEntity( 54.222894, 18.116137, "v31" ) );
		sessionHbn.saveOrUpdate( new GeoEntity( 54.134499, 18.137614, "v32" ) );
		sessionHbn.saveOrUpdate( new GeoEntity( 54.188910, 18.180216, "v33" ) );
		sessionHbn.saveOrUpdate( new GeoEntity( 54.157758, 18.125557, "v34" ) );
		sessionHbn.saveOrUpdate( new GeoEntity( 54.194089, 18.228482, "v35" ) );
		sessionHbn.saveOrUpdate( new GeoEntity( 54.138517, 18.014723, "v36" ) );
		sessionHbn.saveOrUpdate( new GeoEntity( 54.200781, 18.163288, "v37" ) );
		sessionHbn.saveOrUpdate( new GeoEntity( 54.103558, 18.146598, "v38" ) );
		sessionHbn.saveOrUpdate( new GeoEntity( 54.193829, 18.142770, "v39" ) );
		sessionHbn.saveOrUpdate( new GeoEntity( 54.003273, 18.228929, "v40" ) );
		sessionHbn.saveOrUpdate( new GeoEntity( 54.051599, 18.236313, "v41" ) );
		sessionHbn.saveOrUpdate( new GeoEntity( 54.008487, 18.197262, "v42" ) );
		sessionHbn.saveOrUpdate( new GeoEntity( 54.041120, 18.079304, "v43" ) );
		sessionHbn.saveOrUpdate( new GeoEntity( 54.174614, 18.071497, "v44" ) );
		sessionHbn.saveOrUpdate( new GeoEntity( 54.013968, 18.015511, "v45" ) );
		sessionHbn.saveOrUpdate( new GeoEntity( 54.094368, 18.036610, "v46" ) );
		sessionHbn.saveOrUpdate( new GeoEntity( 54.025288, 18.144333, "v47" ) );
		sessionHbn.saveOrUpdate( new GeoEntity( 54.075499, 18.206458, "v48" ) );
		sessionHbn.saveOrUpdate( new GeoEntity( 54.095646, 18.033243, "v49" ) );

		sessionHbn.getTransaction().commit();
		sessionHbn.close();
		System.out.println( "test data saved" );
	}
}
