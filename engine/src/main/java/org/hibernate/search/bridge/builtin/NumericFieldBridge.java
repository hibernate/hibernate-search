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
import org.hibernate.search.query.fieldcache.impl.FieldCacheLoadingType;

/**
 * Stateless field bridges for the 4 different Numeric Field types
 *
 * @author Sanne Grinovero
 * @author Gunnar Morling
 */
public enum NumericFieldBridge implements FieldBridge, TwoWayFieldBridge {

	/**
	 * Persists byte properties in int index fields. Takes care of all the required conversion.
	 */
	BYTE_FIELD_BRIDGE {
		@Override
		public FieldCacheLoadingType getFieldCacheLoadingType() {
			return FieldCacheLoadingType.BYTE_AS_SHORT;
		}

		@Override
		public Object get(final String name, final Document document) {
			final IndexableField field = document.getField( name );
			return field != null ? field.numericValue().byteValue() : null;
		}

		@Override
		protected void applyToLuceneOptions(LuceneOptions luceneOptions, String name, Number value, Document document) {
			super.applyToLuceneOptions( luceneOptions, name, value.intValue(), document );
		}
	},
	/**
	 * Persists short properties in int index fields. Takes care of all the required conversion.
	 */
	SHORT_FIELD_BRIDGE {
		@Override
		public FieldCacheLoadingType getFieldCacheLoadingType() {
			return FieldCacheLoadingType.INT_AS_SHORT;
		}

		@Override
		public Object get(final String name, final Document document) {
			final IndexableField field = document.getField( name );
			return field != null ? field.numericValue().shortValue() : null;
		}

		@Override
		protected void applyToLuceneOptions(LuceneOptions luceneOptions, String name, Number value, Document document) {
			super.applyToLuceneOptions( luceneOptions, name, value.intValue(), document );
		}
	},
	INT_FIELD_BRIDGE {
		@Override
		public FieldCacheLoadingType getFieldCacheLoadingType() {
			return FieldCacheLoadingType.INT;
		}
	},
	FLOAT_FIELD_BRIDGE {
		@Override
		public FieldCacheLoadingType getFieldCacheLoadingType() {
			return FieldCacheLoadingType.FLOAT;
		}
	},
	DOUBLE_FIELD_BRIDGE {
		@Override
		public FieldCacheLoadingType getFieldCacheLoadingType() {
			return FieldCacheLoadingType.DOUBLE;
		}
	},
	LONG_FIELD_BRIDGE {
		@Override
		public FieldCacheLoadingType getFieldCacheLoadingType() {
			return FieldCacheLoadingType.LONG;
		}
	};

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

	public abstract FieldCacheLoadingType getFieldCacheLoadingType();

}
