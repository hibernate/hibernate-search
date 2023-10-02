/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.query.engine;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Facet;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.SortableField;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.engine.ProjectionConstants;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.query.engine.spi.HSQuery;
import org.hibernate.search.query.facet.FacetingRequest;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.hibernate.search.testsupport.junit.SearchITHelper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Test that the HSQuery result cache gets cleared as necessary.
 *
 * @author Yoann Rodiere
 */
class HSQueryResultCacheClearingTest {

	@RegisterExtension
	public final SearchFactoryHolder sfHolder = new SearchFactoryHolder( IndexedEntity.class );

	private final SearchITHelper helper = new SearchITHelper( sfHolder );

	@BeforeEach
	void setUp() {
		helper.add( new IndexedEntity( 0, "zero" ) );
		helper.add( new IndexedEntity( 1, "one" ) );
		helper.add( new IndexedEntity( 2, "two" ) );
	}

	@Test
	void clear_firstResult() {
		HSQuery hsQuery = queryAll();
		helper.assertThatQuery( hsQuery ).matchesExactlyIds( 0, 1, 2 );

		hsQuery.firstResult( 1 );
		helper.assertThatQuery( hsQuery ).matchesExactlyIds( 1, 2 );

		hsQuery.firstResult( 2 );
		helper.assertThatQuery( hsQuery ).matchesExactlyIds( 2 );
	}

	@Test
	void clear_maxResult() {
		HSQuery hsQuery = queryAll();
		helper.assertThatQuery( hsQuery ).matchesExactlyIds( 0, 1, 2 );

		hsQuery.maxResults( 2 );
		helper.assertThatQuery( hsQuery ).matchesExactlyIds( 0, 1 );

		hsQuery.maxResults( 1 );
		helper.assertThatQuery( hsQuery ).matchesExactlyIds( 0 );
	}

	@Test
	void clear_projection() {
		HSQuery hsQuery = queryAll();
		hsQuery.projection( ProjectionConstants.ID );
		helper.assertThatQuery( hsQuery ).matchesExactlySingleProjections( 0, 1, 2 );

		hsQuery.projection( ProjectionConstants.ID, "field" );
		helper.assertThatQuery( hsQuery ).matchesExactlyProjections(
				new Object[][] {
						{ 0, "zero" },
						{ 1, "one" },
						{ 2, "two" },
				} );
	}

	@Test
	void clear_sort() {
		QueryBuilder qb = helper.queryBuilder( IndexedEntity.class );
		HSQuery hsQuery = queryAll();
		helper.assertThatQuery( hsQuery ).matchesExactlyIds( 0, 1, 2 );

		hsQuery.sort( qb.sort().byField( "idSort" ).desc().createSort() );
		helper.assertThatQuery( hsQuery ).matchesExactlyIds( 2, 1, 0 );
	}

	@Test
	void clear_faceting() {
		QueryBuilder qb = helper.queryBuilder( IndexedEntity.class );
		HSQuery hsQuery = queryAll();
		helper.assertThatQuery( hsQuery ).matchesExactlyIds( 0, 1, 2 );

		String facetingRequestName = "facet1";
		FacetingRequest facetingRequest1 = qb.facet()
				.name( facetingRequestName )
				.onField( "facetField" )
				.discrete()
				.createFacetingRequest();
		hsQuery.getFacetManager().enableFaceting( facetingRequest1 );
		helper.assertThatQuery( hsQuery ).facets( facetingRequestName )
				.includes( "zero", 1 )
				.includes( "one", 1 )
				.includes( "two", 1 )
				.only();

		hsQuery.getFacetManager().disableFaceting( facetingRequestName );
		helper.assertThatQuery( hsQuery ).facets( facetingRequestName ).isEmpty();
	}

	private HSQuery queryAll() {
		QueryBuilder qb = helper.queryBuilder( IndexedEntity.class );
		return helper.hsQuery( IndexedEntity.class )
				.sort( qb.sort().byField( "idSort" ).createSort() );
	}

	@Indexed
	private static class IndexedEntity {
		@DocumentId
		@Field(name = "idSort")
		@SortableField(forField = "idSort")
		private Integer id;

		@Field(store = Store.YES, analyze = Analyze.NO)
		@SortableField
		@Field(name = "facetField", analyze = Analyze.NO)
		@Facet(forField = "facetField")
		private String field;

		public IndexedEntity(Integer id, String field) {
			super();
			this.id = id;
			this.field = field;
		}
	}

}
