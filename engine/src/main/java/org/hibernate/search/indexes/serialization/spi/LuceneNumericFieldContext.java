/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.indexes.serialization.spi;

import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.IndexOptions;

/**
 * @author Emmanuel Bernard
 * @author Sanne Grinovero
 */
public class LuceneNumericFieldContext {

	private final FieldType field;
	private final String fieldName;
	private final float fieldBoost;

	public LuceneNumericFieldContext(FieldType field, String fieldName, float fieldBoost) {
		this.field = field;
		this.fieldName = fieldName;
		this.fieldBoost = fieldBoost;
	}

	public String getName() {
		return fieldName;
	}

	public int getPrecisionStep() {
		return field.numericPrecisionStep();
	}

	public SerializableStore getStore() {
		return field.stored() ? SerializableStore.YES : SerializableStore.NO;
	}

	public boolean isIndexed() {
		return field.indexOptions() != IndexOptions.NONE;
	}

	public float getBoost() {
		return fieldBoost;
	}

	public boolean getOmitNorms() {
		return field.omitNorms();
	}

	public boolean getOmitTermFreqAndPositions() {
		return field.indexOptions() == IndexOptions.DOCS;
	}

}
