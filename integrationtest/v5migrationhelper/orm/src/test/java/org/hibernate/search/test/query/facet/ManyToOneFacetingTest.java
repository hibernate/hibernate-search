/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.query.facet;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Iterator;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.query.facet.Facet;
import org.hibernate.search.query.facet.FacetingRequest;
import org.hibernate.search.testsupport.junit.Tags;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.apache.lucene.search.MatchAllDocsQuery;

@Tag(Tags.PORTED_TO_SEARCH_6)
class ManyToOneFacetingTest extends AbstractFacetTest {
	private final String indexFieldName = "companyFacilities.country";
	private final String facetName = "countryFacility";

	@Test
	void testAllIndexedManyToOneValuesGetCounted() {
		FacetingRequest request = queryBuilder( Company.class ).facet()
				.name( facetName )
				.onField( indexFieldName )
				.discrete()
				.includeZeroCounts( true )
				.createFacetingRequest();
		FullTextQuery query = queryCompanyWithFacet( request );

		List<Facet> facetList = query.getFacetManager().getFacets( facetName );
		assertThat( facetList ).as( "Wrong number of facets" ).hasSize( 2 );

		// check count in facet
		Iterator<Facet> itr = facetList.iterator();
		while ( itr.hasNext() ) {
			Facet item = itr.next();
			assertThat( item.getCount() ).as( "Wrong count of facet" ).isEqualTo( 1 );
		}

	}

	private FullTextQuery queryCompanyWithFacet(FacetingRequest request) {
		FullTextQuery query = fullTextSession.createFullTextQuery( new MatchAllDocsQuery() );
		query.getFacetManager().enableFaceting( request );
		assertThat( query.getResultSize() ).as( "Wrong number of query matches" ).isEqualTo( 1 );
		return query;
	}

	@Override
	public void loadTestData(Session session) {
		Transaction tx = session.beginTransaction();

		Company acme = new Company( "ACME" );

		CompanyFacility usFacility = new CompanyFacility( "US" );
		usFacility.setCompany( acme );
		acme.addCompanyFacility( usFacility );

		CompanyFacility indiaFacility = new CompanyFacility( "INDIA" );
		indiaFacility.setCompany( acme );
		acme.addCompanyFacility( indiaFacility );

		session.save( acme );

		tx.commit();
		session.clear();
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] { Company.class, CompanyFacility.class };
	}
}
