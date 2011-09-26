/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.search.test.query.facet;

import java.util.List;

import org.apache.lucene.search.Query;

import org.hibernate.Session;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.query.facet.Facet;
import org.hibernate.search.query.facet.FacetingRequest;

/**
 * @author Hardy Ferentschik <hardy@hibernate.org>
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class EdgeCaseFacetTest extends AbstractFacetTest {
	private final String indexFieldName = "cubicCapacity";
	private final String facetName = "ccs";

	public void testFacetingOnEmptyIndex() throws Exception {
		FacetingRequest request = queryBuilder( Car.class ).facet()
				.name( facetName )
				.onField( indexFieldName )
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
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Car.class
		};
	}

	@Override
	public void loadTestData(Session session) {
		//empty index
	}
}
