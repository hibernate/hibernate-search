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

	private final DocumentFieldPath path;

	private final FacetEncodingType encoding;

	private FacetMetadata(Builder builder) {
		this.path = builder.path;
		this.encoding = builder.encoding;
	}

	/**
	 * @return The full name of the facet field, including any indexed-embedded prefix. Equivalent to {@code #getPath().getAbsoluteName()}.
	 */
	public String getAbsoluteName() {
		return path.getAbsoluteName();
	}

	/**
	 * @return The path from the document root to this field.
	 */
	public DocumentFieldPath getPath() {
		return path;
	}

	public FacetEncodingType getEncoding() {
		return encoding;
	}

	public static class Builder {
		// required parameters
		private final DocumentFieldPath path;

		// optional parameters
		private FacetEncodingType encoding = FacetEncodingType.AUTO;

		public Builder(DocumentFieldPath path) {
			this.path = path;
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
				"path='" + path + '\'' +
				", encoding=" + encoding +
				'}';
	}
}
