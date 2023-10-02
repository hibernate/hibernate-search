/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.query.facet;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.query.facet.Facet;
import org.hibernate.search.query.facet.FacetingRequest;
import org.hibernate.search.testsupport.junit.Tags;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.apache.lucene.search.Query;

/**
 * @author Hardy Ferentschik
 * @author Emmanuel Bernard
 */
@Tag(Tags.PORTED_TO_SEARCH_6)
class EdgeCaseFacetTest extends AbstractFacetTest {
	private final String facetName = "ccs";

	@Test
	void testFacetingOnEmptyIndex() {
		FacetingRequest request = queryBuilder( Car.class ).facet()
				.name( facetName )
				.onField( Car.CUBIC_CAPACITY_STRING_FACET_STRING_ENCODING )
				.discrete()
				.createFacetingRequest();
		FullTextQuery query = queryHondaWithFacet( request );

		List<Facet> facetList = query.getFacetManager().getFacets( facetName );
		assertThat( facetList ).as( "Wrong number of facets" ).isEmpty();
	}

	private FullTextQuery queryHondaWithFacet(FacetingRequest request) {
		Query luceneQuery = queryBuilder( Car.class ).keyword().onField( "make" ).matching( "Honda" ).createQuery();
		FullTextQuery query = fullTextSession.createFullTextQuery( luceneQuery, Car.class );
		query.getFacetManager().enableFaceting( request );
		assertThat( query.getResultSize() ).as( "Wrong number of query matches" ).isEqualTo( 0 );
		return query;
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Car.class
		};
	}

	@Override
	public void loadTestData(Session session) {
		//empty index
	}
}
