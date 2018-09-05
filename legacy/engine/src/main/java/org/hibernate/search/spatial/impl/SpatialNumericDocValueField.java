/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.spatial.impl;

import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.DocValuesType;
import org.apache.lucene.index.IndexOptions;

/**
 * This class is equivalent to {@code DoubleDocValuesField} except that we set the property indexed to false explicitly.
 *
 * @see org.apache.lucene.document.DoubleDocValuesField
 * @author Davide D'Alto
 */
public class SpatialNumericDocValueField extends Field {

	public static final FieldType TYPE = createSpatialFieldType();

	private static FieldType createSpatialFieldType() {
		FieldType type = new FieldType();
		type.setIndexOptions( IndexOptions.NONE );
		type.setDocValuesType( DocValuesType.NUMERIC );
		type.freeze();
		return type;
	}

	public SpatialNumericDocValueField(String name, Double value) {
		super( name, TYPE );
		fieldsData = Double.doubleToRawLongBits( value );
	}

	@Override
	public void setDoubleValue(double value) {
		super.setLongValue( Double.doubleToRawLongBits( value ) );
	}

	@Override
	public void setLongValue(long value) {
		throw new IllegalArgumentException( "cannot change value type from Double to Long" );
	}
}
