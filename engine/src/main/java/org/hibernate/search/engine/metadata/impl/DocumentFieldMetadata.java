/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.metadata.impl;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Field;

import org.hibernate.search.annotations.NumericField;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.engine.impl.nullencoding.NotEncodingCodec;
import org.hibernate.search.engine.impl.nullencoding.NullMarkerCodec;

import static org.hibernate.search.metadata.NumericFieldSettingsDescriptor.NumericEncodingType;

/**
 * Encapsulating the metadata for a single field within a Lucene {@code Document}.
 *
 * @author Hardy Ferentschik
 */
@SuppressWarnings("deprecation")
public class DocumentFieldMetadata {
	private final String fieldName;
	private final Store store;
	private final Field.Index index;
	private final Field.TermVector termVector;
	private final FieldBridge fieldBridge;
	private final Float boost;
	private final Analyzer analyzer;
	private final boolean isId;
	private final boolean isIdInEmbedded;
	private final NullMarkerCodec nullMarkerCodec;
	private final boolean isNumeric;
	private final boolean isSpatial;
	private final int precisionStep;
	private final NumericEncodingType numericEncodingType;
	private final Set<FacetMetadata> facetMetadata;

	private DocumentFieldMetadata(Builder builder) {
		this.fieldName = builder.fieldName;
		this.store = builder.store;
		this.index = builder.index;
		this.termVector = builder.termVector;
		this.fieldBridge = builder.fieldBridge;
		this.boost = builder.boost;
		this.analyzer = builder.analyzer;
		this.isId = builder.isId;
		this.isIdInEmbedded = builder.isIdInEmbedded;
		this.nullMarkerCodec = builder.nullMarkerCodec;
		this.isNumeric = builder.isNumeric;
		this.isSpatial = builder.isSpatial;
		this.precisionStep = builder.precisionStep;
		this.numericEncodingType = builder.numericEncodingType;
		this.facetMetadata = Collections.unmodifiableSet( builder.facetMetadata );
	}

	public String getName() {
		return fieldName;
	}

	public boolean isId() {
		return isId;
	}

	public boolean isIdInEmbedded() {
		return isIdInEmbedded;
	}

	public Store getStore() {
		return store;
	}

	public Field.Index getIndex() {
		return index;
	}

	public Field.TermVector getTermVector() {
		return termVector;
	}

	public FieldBridge getFieldBridge() {
		return fieldBridge;
	}

	public Float getBoost() {
		return boost;
	}

	public Analyzer getAnalyzer() {
		return analyzer;
	}

	public String indexNullAs() {
		return nullMarkerCodec.nullRepresentedAsString();
	}

	public boolean isNumeric() {
		return isNumeric;
	}

	public boolean isSpatial() {
		return isSpatial;
	}

	public Integer getPrecisionStep() {
		return precisionStep;
	}

	public NumericEncodingType getNumericEncodingType() {
		return numericEncodingType;
	}

	public boolean hasFacets() {
		return !facetMetadata.isEmpty();
	}

	public String getFieldName() {
		return fieldName;
	}

	public Set<FacetMetadata> getFacetMetadata() {
		return facetMetadata;
	}

	@Override
	public String toString() {
		return "DocumentFieldMetadata{" +
				"fieldName='" + fieldName + '\'' +
				", store=" + store +
				", index=" + index +
				", termVector=" + termVector +
				", fieldBridge=" + fieldBridge +
				", boost=" + boost +
				", analyzer=" + analyzer +
				", isId=" + isId +
				", isIdInEmbedded=" + isIdInEmbedded +
				", nullToken='" + nullMarkerCodec.nullRepresentedAsString() + '\'' +
				", isNumeric=" + isNumeric +
				", isSpatial=" + isSpatial +
				", precisionStep=" + precisionStep +
				", numericEncodingType=" + numericEncodingType +
				", facetMetadata=" + facetMetadata +
				'}';
	}

	public static class Builder {
		// required parameters
		private final String fieldName;
		private final Store store;
		private final Field.Index index;
		private final Field.TermVector termVector;

		// optional parameters
		private FieldBridge fieldBridge;
		private Float boost;
		private Analyzer analyzer;
		private boolean isId;
		private boolean isIdInEmbedded;
		private boolean isNumeric;
		private boolean isSpatial;
		private int precisionStep = NumericField.PRECISION_STEP_DEFAULT;
		private NumericEncodingType numericEncodingType;
		private Set<FacetMetadata> facetMetadata;
		private NullMarkerCodec nullMarkerCodec = NotEncodingCodec.SINGLETON;

		public Builder(String fieldName,
				Store store,
				Field.Index index,
				Field.TermVector termVector) {

			this.fieldName = fieldName;
			this.store = store;
			this.index = index;
			this.termVector = termVector;
			this.facetMetadata = new HashSet<>( 1 ); // the most common case is a single facet
		}

		public Builder fieldBridge(FieldBridge fieldBridge) {
			this.fieldBridge = fieldBridge;
			return this;
		}

		public Builder boost(Float boost) {
			this.boost = boost;
			return this;
		}

		public Builder analyzer(Analyzer analyzer) {
			this.analyzer = analyzer;
			return this;
		}

		public Builder id() {
			this.isId = true;
			return this;
		}

		public Builder idInEmbedded() {
			this.isIdInEmbedded = true;
			return this;
		}

		public Builder indexNullAs(NullMarkerCodec nullMarkerCodec) {
			this.nullMarkerCodec = nullMarkerCodec;
			return this;
		}

		public Builder numeric() {
			this.isNumeric = true;
			return this;
		}

		public Builder spatial() {
			this.isSpatial = true;
			return this;
		}

		public Builder precisionStep(int precisionStep) {
			this.precisionStep = precisionStep;
			return this;
		}

		public Builder numericEncodingType(NumericEncodingType numericEncodingType) {
			this.numericEncodingType = numericEncodingType;
			return this;
		}

		public Builder addFacetMetadata(FacetMetadata facetMetadata) {
			this.facetMetadata.add( facetMetadata );
			return this;
		}

		public DocumentFieldMetadata build() {
			return new DocumentFieldMetadata( this );
		}

		@Override
		public String toString() {
			return "Builder{" +
					"fieldName='" + fieldName + '\'' +
					'}';
		}
	}

}


