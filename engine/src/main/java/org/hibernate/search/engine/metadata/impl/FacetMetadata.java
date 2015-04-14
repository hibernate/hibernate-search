/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.metadata.impl;

import org.hibernate.search.annotations.FacetEncodingType;

/**
 * Encapsulating the metadata for a facet on a given document field.
 *
 * @author Hardy Ferentschik
 */
public class FacetMetadata {

	private final String facetName;

	private final FacetEncodingType encoding;

	private FacetMetadata(Builder builder) {
		this.facetName = builder.facetName;
		this.encoding = builder.encoding;
	}

	public String getFacetName() {
		return facetName;
	}

	public FacetEncodingType getEncoding() {
		return encoding;
	}

	public static class Builder {
		// required parameters
		private final String facetName;

		// optional parameters
		private FacetEncodingType encoding = FacetEncodingType.AUTO;

		public Builder(String facetName) {
			this.facetName = facetName;
		}

		public void setFacetEncoding(FacetEncodingType encoding) {
			this.encoding = encoding;
		}

		public FacetMetadata build() {
			return new FacetMetadata( this );
		}
	}

	@Override
	public String toString() {
		return "FacetMetadata{" +
				"facetName='" + facetName + '\'' +
				", encoding=" + encoding +
				'}';
	}
}
