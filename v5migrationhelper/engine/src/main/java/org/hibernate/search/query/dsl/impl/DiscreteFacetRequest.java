/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.query.dsl.impl;

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
	public Facet createFacet(String absoluteFieldPath, String value, int count) {
		return new SimpleFacet( getFacetingName(), getFieldName(), absoluteFieldPath, value, count );
	}

	static class SimpleFacet extends AbstractFacet {
		SimpleFacet(String facetingName, String facetFieldName, String sourceFieldName, String value, int count) {
			super( facetingName, facetFieldName, sourceFieldName, value, count );
		}
	}
}
