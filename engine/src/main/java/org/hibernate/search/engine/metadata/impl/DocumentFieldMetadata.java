/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.metadata.impl;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.hibernate.search.analyzer.spi.AnalyzerReference;
import org.hibernate.search.annotations.NumericField;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.engine.metadata.impl.DocumentFieldMetadata.Builder;
import org.hibernate.search.engine.nulls.codec.impl.NotEncodingCodec;
import org.hibernate.search.engine.nulls.codec.impl.NullMarkerCodec;

import static org.hibernate.search.metadata.NumericFieldSettingsDescriptor.NumericEncodingType;

/**
 * Encapsulating the metadata for a single field within a Lucene {@code Document}.
 *
 * @author Hardy Ferentschik
 */
@SuppressWarnings("deprecation")
public class DocumentFieldMetadata implements PartialDocumentFieldMetadata {
	private final BackReference<TypeMetadata> sourceType;
	private final BackReference<PropertyMetadata> sourceProperty;

	private final DocumentFieldPath path;
	private final Store store;
	private final Field.Index index;
	private final Field.TermVector termVector;
	private final FieldBridge fieldBridge;
	private final Float boost;
	private final AnalyzerReference analyzerReference;
	private final boolean isId;
	private final boolean isIdInEmbedded;
	private final NullMarkerCodec nullMarkerCodec;
	private final boolean isNumeric;
	private final boolean isSpatial;
	private final int precisionStep;
	private final NumericEncodingType numericEncodingType;
	private final Set<FacetMetadata> facetMetadata;

	/**
	 * Fields explicitly declared by {@link org.hibernate.search.bridge.MetadataProvidingFieldBridge}s.
	 * <p>
	 * Note: This is only to be used for validation / schema creation in ES, don't use it to drive invocation of field
	 * bridges at indexing time!
	 */
	private final Map<String, BridgeDefinedField> bridgeDefinedFields;

	private DocumentFieldMetadata(Builder builder) {
		this.sourceType = builder.sourceType;
		this.sourceProperty = builder.sourceProperty;

		this.path = builder.path;
		this.store = builder.store;
		this.index = builder.index;
		this.termVector = builder.termVector;
		this.fieldBridge = builder.fieldBridge;
		this.boost = builder.boost;
		this.analyzerReference = builder.analyzerReference;
		this.isId = builder.isId;
		this.isIdInEmbedded = builder.isIdInEmbedded;
		this.nullMarkerCodec = builder.nullMarkerCodec;
		this.isNumeric = builder.isNumeric;
		this.isSpatial = builder.isSpatial;
		this.precisionStep = builder.precisionStep;
		this.numericEncodingType = builder.numericEncodingType;
		this.facetMetadata = Collections.unmodifiableSet( builder.facetMetadata );
		this.bridgeDefinedFields = Collections.unmodifiableMap( builder.bridgeDefinedFields );
	}

	/**
	 * @return The type from which the value for this field is extracted. This is not
	 * the type of the actual value, since the value might be extracted by accessing a
	 * {@link #getSourceProperty() property} on this type.
	 */
	public TypeMetadata getSourceType() {
		return sourceType.get();
	}

	/**
	 * @return The property from which the value for this field is extracted.
	 * {@code null} for class bridges.
	 */
	@Override
	public PropertyMetadata getSourceProperty() {
		return sourceProperty.get();
	}

	/**
	 * @return The full name of this field, including any indexed-embedded prefix. Equivalent to {@code #getPath().getAbsoluteName()}.
	 */
	public String getAbsoluteName() {
		return path.getAbsoluteName();
	}

	/**
	 * @return The path from the document root to this field.
	 */
	@Override
	public DocumentFieldPath getPath() {
		return path;
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

	@Override
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

	public AnalyzerReference getAnalyzerReference() {
		return analyzerReference;
	}

	public NullMarkerCodec getNullMarkerCodec() {
		return nullMarkerCodec;
	}

	@Override
	public boolean isNumeric() {
		return isNumeric;
	}

	public boolean isSpatial() {
		return isSpatial;
	}

	public Integer getPrecisionStep() {
		return precisionStep;
	}

	@Override
	public NumericEncodingType getNumericEncodingType() {
		return numericEncodingType;
	}

	public boolean hasFacets() {
		return !facetMetadata.isEmpty();
	}

	public Set<FacetMetadata> getFacetMetadata() {
		return facetMetadata;
	}

	@Override
	public Map<String, BridgeDefinedField> getBridgeDefinedFields() {
		return bridgeDefinedFields;
	}

	@Override
	public String toString() {
		return "DocumentFieldMetadata{" +
				"path='" + path + '\'' +
				", sourceType='" + sourceType + '\'' +
				", sourceProperty='" + sourceProperty + '\'' +
				", store=" + store +
				", index=" + index +
				", termVector=" + termVector +
				", fieldBridge=" + fieldBridge +
				", boost=" + boost +
				", analyzer=" + analyzerReference +
				", isId=" + isId +
				", isIdInEmbedded=" + isIdInEmbedded +
				", nullMarkerCodec='" + nullMarkerCodec + '\'' +
				", isNumeric=" + isNumeric +
				", isSpatial=" + isSpatial +
				", precisionStep=" + precisionStep +
				", numericEncodingType=" + numericEncodingType +
				", facetMetadata=" + facetMetadata +
				'}';
	}

	public static class Builder implements PartialDocumentFieldMetadata {
		protected final BackReference<DocumentFieldMetadata> resultReference = new BackReference<>();

		// required parameters
		private final BackReference<TypeMetadata> sourceType;
		private final BackReference<PropertyMetadata> sourceProperty;
		private final PartialPropertyMetadata partialSourceProperty;
		private final DocumentFieldPath path;
		private final Store store;
		private Field.Index index;
		private final Field.TermVector termVector;

		// optional parameters
		private FieldBridge fieldBridge;
		private Float boost;
		private AnalyzerReference analyzerReference;
		private boolean isId;
		private boolean isIdInEmbedded;
		private boolean isNumeric;
		private boolean isSpatial;
		private int precisionStep = NumericField.PRECISION_STEP_DEFAULT;
		private NumericEncodingType numericEncodingType;
		private Set<FacetMetadata> facetMetadata;
		private NullMarkerCodec nullMarkerCodec = NotEncodingCodec.SINGLETON;
		private final Map<String, BridgeDefinedField> bridgeDefinedFields;

		public Builder(BackReference<TypeMetadata> sourceType,
				BackReference<PropertyMetadata> sourceProperty,
				PartialPropertyMetadata partialSourceProperty,
				DocumentFieldPath path,
				Store store,
				Field.Index index,
				Field.TermVector termVector) {
			this.sourceType = sourceType;
			this.sourceProperty = sourceProperty;
			this.partialSourceProperty = partialSourceProperty;
			this.path = path;
			this.store = store;
			this.index = index;
			this.termVector = termVector;
			this.facetMetadata = new LinkedHashSet<>( 1 ); // the most common case is a single facet
			this.bridgeDefinedFields = new LinkedHashMap<>();
		}

		@Override
		public PartialPropertyMetadata getSourceProperty() {
			return partialSourceProperty;
		}

		@Override
		public DocumentFieldPath getPath() {
			return path;
		}

		public String getAbsoluteName() {
			return path.getAbsoluteName();
		}

		public String getRelativeName() {
			return path.getRelativeName();
		}

		@Override
		public Index getIndex() {
			return index;
		}

		public Builder index(Index index) {
			this.index = index;
			return this;
		}

		public Builder fieldBridge(FieldBridge fieldBridge) {
			this.fieldBridge = fieldBridge;
			return this;
		}

		public Builder boost(Float boost) {
			this.boost = boost;
			return this;
		}

		public Builder analyzerReference(AnalyzerReference analyzerReference) {
			this.analyzerReference = analyzerReference;
			return this;
		}

		@Override
		public AnalyzerReference getAnalyzerReference() {
			return analyzerReference;
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

		@Override
		public boolean isNumeric() {
			return isNumeric;
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

		@Override
		public NumericEncodingType getNumericEncodingType() {
			return numericEncodingType;
		}

		public Builder addFacetMetadata(FacetMetadata facetMetadata) {
			this.facetMetadata.add( facetMetadata );
			return this;
		}

		public Builder addBridgeDefinedField(BridgeDefinedField bridgeDefinedField) {
			this.bridgeDefinedFields.put( bridgeDefinedField.getAbsoluteName(), bridgeDefinedField );
			return this;
		}

		@Override
		public Map<String, BridgeDefinedField> getBridgeDefinedFields() {
			return Collections.unmodifiableMap( bridgeDefinedFields );
		}

		public BackReference<DocumentFieldMetadata> getResultReference() {
			return resultReference;
		}

		public DocumentFieldMetadata build() {
			DocumentFieldMetadata result = new DocumentFieldMetadata( this );
			resultReference.initialize( result );
			return result;
		}

		@Override
		public String toString() {
			return "Builder{" +
					"path='" + path + '\'' +
					'}';
		}
	}

}


