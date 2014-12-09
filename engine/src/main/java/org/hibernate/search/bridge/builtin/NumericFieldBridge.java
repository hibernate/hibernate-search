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
 */
public enum NumericFieldBridge implements FieldBridge, TwoWayFieldBridge {

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
	public final Object get(final String name, final Document document) {
		final IndexableField field = document.getField( name );
		if ( field != null ) {
			return field.numericValue();
		}
		else {
			return null;
		}
	}

	protected final void applyToLuceneOptions(LuceneOptions luceneOptions, String name, Number value, Document document) {
		luceneOptions.addNumericFieldToDocument( name, value, document );
	}

	public abstract FieldCacheLoadingType getFieldCacheLoadingType();

}
