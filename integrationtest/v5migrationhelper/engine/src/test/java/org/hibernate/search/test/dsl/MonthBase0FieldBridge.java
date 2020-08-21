/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.dsl;

import org.apache.lucene.document.Document;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.MetadataProvidingFieldBridge;
import org.hibernate.search.bridge.spi.FieldMetadataBuilder;
import org.hibernate.search.bridge.spi.FieldType;

/**
 * Example of a MetadataProvidingFieldBridge overriding the field type to make it numeric.
 */
public class MonthBase0FieldBridge implements MetadataProvidingFieldBridge {

	@Override
	public void configureFieldMetadata(String name, FieldMetadataBuilder builder) {
		builder.field( name, FieldType.INTEGER );
	}

	@Override
	public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
		luceneOptions.addNumericFieldToDocument( name, (Integer) value - 1, document );
	}

}
