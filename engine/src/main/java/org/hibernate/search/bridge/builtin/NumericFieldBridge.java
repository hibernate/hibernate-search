/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.bridge.builtin;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.TwoWayFieldBridge;
import org.hibernate.search.bridge.builtin.nullencoding.impl.NumericDoubleNullCodec;
import org.hibernate.search.bridge.builtin.nullencoding.impl.NumericFloatNullCodec;
import org.hibernate.search.bridge.builtin.nullencoding.impl.NumericIntegerNullCodec;
import org.hibernate.search.bridge.builtin.nullencoding.impl.NumericLongNullCodec;
import org.hibernate.search.bridge.spi.EncodingBridge;
import org.hibernate.search.bridge.spi.IgnoreAnalyzerBridge;
import org.hibernate.search.bridge.spi.NullMarkerCodec;
import org.hibernate.search.metadata.NumericFieldSettingsDescriptor.NumericEncodingType;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Stateless field bridges for the conversion of numbers to numeric field values.
 *
 * @author Sanne Grinovero
 * @author Gunnar Morling
 */
public enum NumericFieldBridge implements FieldBridge, TwoWayFieldBridge, EncodingBridge, IgnoreAnalyzerBridge {

	/**
	 * Persists byte properties in int index fields. Takes care of all the required conversion.
	 */
	BYTE_FIELD_BRIDGE {
		@Override
		public Object get(final String name, final Document document) {
			final IndexableField field = document.getField( name );
			return field != null ? field.numericValue().byteValue() : null;
		}

		@Override
		protected void applyToLuceneOptions(LuceneOptions luceneOptions, String name, Number value, Document document) {
			super.applyToLuceneOptions( luceneOptions, name, value.intValue(), document );
		}

		@Override
		public NumericEncodingType getEncodingType() {
			return NumericEncodingType.INTEGER;
		}

		@Override
		public NullMarkerCodec createNullMarkerCodec(String indexNullAs) throws IllegalArgumentException {
			return INT_FIELD_BRIDGE.createNullMarkerCodec( indexNullAs );
		}
	},
	/**
	 * Persists short properties in int index fields. Takes care of all the required conversion.
	 */
	SHORT_FIELD_BRIDGE {
		@Override
		public Object get(final String name, final Document document) {
			final IndexableField field = document.getField( name );
			return field != null ? field.numericValue().shortValue() : null;
		}

		@Override
		protected void applyToLuceneOptions(LuceneOptions luceneOptions, String name, Number value, Document document) {
			super.applyToLuceneOptions( luceneOptions, name, value.intValue(), document );
		}

		@Override
		public NumericEncodingType getEncodingType() {
			return NumericEncodingType.INTEGER;
		}

		@Override
		public NullMarkerCodec createNullMarkerCodec(String indexNullAs) throws IllegalArgumentException {
			return INT_FIELD_BRIDGE.createNullMarkerCodec( indexNullAs );
		}
	},
	/**
	 * Persists int properties in int index fields. Takes care of all the required conversion.
	 */
	INT_FIELD_BRIDGE {
		@Override
		public NumericEncodingType getEncodingType() {
			return NumericEncodingType.INTEGER;
		}

		@Override
		public NullMarkerCodec createNullMarkerCodec(String indexNullAs) throws IllegalArgumentException {
			try {
				return new NumericIntegerNullCodec( Integer.parseInt( indexNullAs ) );
			}
			catch (NumberFormatException e) {
				throw LOG.invalidNullMarkerForInteger( e );
			}
		}
	},
	/**
	 * Persists float properties in float index fields. Takes care of all the required conversion.
	 */
	FLOAT_FIELD_BRIDGE {
		@Override
		public NumericEncodingType getEncodingType() {
			return NumericEncodingType.FLOAT;
		}

		@Override
		public NullMarkerCodec createNullMarkerCodec(String indexNullAs) throws IllegalArgumentException {
			try {
				return new NumericFloatNullCodec( Float.parseFloat( indexNullAs ) );
			}
			catch (NumberFormatException e) {
				throw LOG.invalidNullMarkerForFloat( e );
			}
		}
	},
	/**
	 * Persists double properties in double index fields. Takes care of all the required conversion.
	 */
	DOUBLE_FIELD_BRIDGE {
		@Override
		public NumericEncodingType getEncodingType() {
			return NumericEncodingType.DOUBLE;
		}

		@Override
		public NullMarkerCodec createNullMarkerCodec(String indexNullAs) throws IllegalArgumentException {
			try {
				return new NumericDoubleNullCodec( Double.parseDouble( indexNullAs ) );
			}
			catch (NumberFormatException e) {
				throw LOG.invalidNullMarkerForDouble( e );
			}
		}
	},
	/**
	 * Persists long properties in long index fields. Takes care of all the required conversion.
	 */
	LONG_FIELD_BRIDGE {
		@Override
		public NumericEncodingType getEncodingType() {
			return NumericEncodingType.LONG;
		}

		@Override
		public NullMarkerCodec createNullMarkerCodec(String indexNullAs) throws IllegalArgumentException {
			try {
				return new NumericLongNullCodec( Long.parseLong( indexNullAs ) );
			}
			catch (NumberFormatException e) {
				throw LOG.invalidNullMarkerForLong( e );
			}
		}
	};

	private static final Log LOG = LoggerFactory.make( Log.class );

	@Override
	public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
		if ( value == null ) {
			if ( luceneOptions.indexNullAs() != null ) {
				luceneOptions.addFieldToDocument( name, luceneOptions.indexNullAs(), document );
			}
		}
		else {
			applyToLuceneOptions( luceneOptions, name, (Number)value, document );
		}
	}

	@Override
	public final String objectToString(final Object object) {
		return object == null ? null : object.toString();
	}

	@Override
	public Object get(final String name, final Document document) {
		final IndexableField field = document.getField( name );
		if ( field != null ) {
			return field.numericValue();
		}
		else {
			return null;
		}
	}

	protected void applyToLuceneOptions(LuceneOptions luceneOptions, String name, Number value, Document document) {
		luceneOptions.addNumericFieldToDocument( name, value, document );
	}

}
