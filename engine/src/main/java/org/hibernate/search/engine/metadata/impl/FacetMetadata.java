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

	private final BackReference<DocumentFieldMetadata> sourceField;

	private final DocumentFieldPath path;

	private final boolean encodingAuto;

	private final FacetEncodingType encoding;

	private FacetMetadata(Builder builder) {
		this.sourceField = builder.sourceField;
		this.path = builder.path;
		this.encodingAuto = builder.encodingAuto;
		this.encoding = builder.encoding;
	}

	/**
	 * @return The {@link DocumentFieldMetadata} to which the facet applies.
	 */
	public DocumentFieldMetadata getSourceField() {
		return sourceField.get();
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

	public boolean isEncodingAuto() {
		return encodingAuto;
	}

	public FacetEncodingType getEncoding() {
		return encoding;
	}

	public static class Builder {
		// required parameters
		private final BackReference<DocumentFieldMetadata> sourceField;
		private final DocumentFieldPath path;

		// optional parameters
		private FacetEncodingType encoding = FacetEncodingType.AUTO;
		private boolean encodingAuto = true;

		public Builder(BackReference<DocumentFieldMetadata> sourceField,
				DocumentFieldPath path) {
			this.sourceField = sourceField;
			this.path = path;
		}

		public void setFacetEncoding(FacetEncodingType encoding) {
			this.encoding = encoding;
		}

		public void setFacetEncodingAuto(boolean encodingAuto) {
			this.encodingAuto = encodingAuto;
		}

		public FacetMetadata build() {
			return new FacetMetadata( this );
		}
	}

	@Override
	public String toString() {
		return "FacetMetadata{" +
				"sourceField='" + sourceField + '\'' +
				"path='" + path + '\'' +
				", encoding=" + encoding +
				'}';
	}
}
