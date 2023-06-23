/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.query.facet;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.query.facet.Facet;
import org.hibernate.search.query.facet.FacetingRequest;
import org.hibernate.search.testsupport.junit.PortedToSearch6;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import org.apache.lucene.search.Query;

/**
 * @author Hardy Ferentschik
 * @author Emmanuel Bernard
 */
@Category(PortedToSearch6.class)
public class EdgeCaseFacetTest extends AbstractFacetTest {
	private final String facetName = "ccs";

	@Test
	public void testFacetingOnEmptyIndex() throws Exception {
		FacetingRequest request = queryBuilder( Car.class ).facet()
				.name( facetName )
				.onField( Car.CUBIC_CAPACITY_STRING_FACET_STRING_ENCODING )
				.discrete()
				.createFacetingRequest();
		FullTextQuery query = queryHondaWithFacet( request );

		List<Facet> facetList = query.getFacetManager().getFacets( facetName );
		assertEquals( "Wrong number of facets", 0, facetList.size() );
	}

	private FullTextQuery queryHondaWithFacet(FacetingRequest request) {
		Query luceneQuery = queryBuilder( Car.class ).keyword().onField( "make" ).matching( "Honda" ).createQuery();
		FullTextQuery query = fullTextSession.createFullTextQuery( luceneQuery, Car.class );
		query.getFacetManager().enableFaceting( request );
		assertEquals( "Wrong number of query matches", 0, query.getResultSize() );
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
