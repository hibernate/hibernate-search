/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.query.dsl.impl;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.hibernate.search.query.facet.Facet;

/**
 * A facet request for string based fields.
 *
 * @author Hardy Ferentschik
 */
public class DiscreteFacetRequest extends FacetingRequestImpl {
	DiscreteFacetRequest(String name, String fieldName) {
		super( name, fieldName );
	}

	@Override
	public Class<?> getFacetValueType() {
		return String[].class;
	}

	@Override
	public Facet createFacet(String value, int count) {
		return new SimpleFacet( getFacetingName(), getFieldName(), value, count );
	}

	static class SimpleFacet extends AbstractFacet {
		SimpleFacet(String facetingName, String fieldName, String value, int count) {
			super( facetingName, fieldName, value, count );
		}

		@Override
		public Query getFacetQuery() {
			return new TermQuery( new Term( getFieldName(), getValue() ) );
		}
	}
}
