/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.query.facet;

import static org.junit.Assert.assertEquals;

import java.util.Iterator;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.query.facet.Facet;
import org.hibernate.search.query.facet.FacetingRequest;
import org.hibernate.search.testsupport.junit.PortedToSearch6;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import org.apache.lucene.search.MatchAllDocsQuery;

@Category(PortedToSearch6.class)
public class ManyToOneFacetingTest extends AbstractFacetTest {
	private final String indexFieldName = "companyFacilities.country";
	private final String facetName = "countryFacility";

	@Test
	public void testAllIndexedManyToOneValuesGetCounted() throws Exception {
		FacetingRequest request = queryBuilder( Company.class ).facet()
				.name( facetName )
				.onField( indexFieldName )
				.discrete()
				.includeZeroCounts( true )
				.createFacetingRequest();
		FullTextQuery query = queryCompanyWithFacet( request );

		List<Facet> facetList = query.getFacetManager().getFacets( facetName );
		assertEquals( "Wrong number of facets", 2, facetList.size() );

		// check count in facet
		Iterator<Facet> itr = facetList.iterator();
		while ( itr.hasNext() ) {
			Facet item = itr.next();
			assertEquals( "Wrong count of facet", 1, item.getCount() );
		}

	}

	private FullTextQuery queryCompanyWithFacet(FacetingRequest request) {
		FullTextQuery query = fullTextSession.createFullTextQuery( new MatchAllDocsQuery() );
		query.getFacetManager().enableFaceting( request );
		assertEquals( "Wrong number of query matches", 1, query.getResultSize() );
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
