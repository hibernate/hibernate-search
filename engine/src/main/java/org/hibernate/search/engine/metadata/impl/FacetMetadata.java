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

	private final String absoluteName;

	private final String relativeName;

	private final FacetEncodingType encoding;

	private FacetMetadata(Builder builder) {
		this.absoluteName = builder.absoluteName;
		this.relativeName = builder.relativeName;
		this.encoding = builder.encoding;
	}

	/**
	 * @return The full name of the facet field, including any indexed-embedded prefix.
	 */
	public String getAbsoluteName() {
		return absoluteName;
	}

	/**
	 * @return The name of the facet field excluding any indexed-embedded prefix.
	 */
	public String getRelativeName() {
		return relativeName;
	}

	public FacetEncodingType getEncoding() {
		return encoding;
	}

	public static class Builder {
		// required parameters
		private final String absoluteName;
		private final String relativeName;

		// optional parameters
		private FacetEncodingType encoding = FacetEncodingType.AUTO;

		public Builder(String absoluteName, String relativeName) {
			this.absoluteName = absoluteName;
			this.relativeName = relativeName;
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
				"absoluteName='" + absoluteName + '\'' +
				"relativeName='" + relativeName + '\'' +
				", encoding=" + encoding +
				'}';
	}
}
