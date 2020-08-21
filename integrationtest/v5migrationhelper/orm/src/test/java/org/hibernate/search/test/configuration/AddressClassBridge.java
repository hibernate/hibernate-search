/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.configuration;

import org.apache.lucene.document.Document;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.MetadataProvidingFieldBridge;
import org.hibernate.search.bridge.spi.FieldMetadataBuilder;
import org.hibernate.search.bridge.spi.FieldType;

/**
 * @author Emmanuel Bernard
 */
public class AddressClassBridge implements MetadataProvidingFieldBridge {
	@Override
	public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
		luceneOptions.addFieldToDocument( "AddressClassBridge", "Applied!", document );
	}

	@Override
	public void configureFieldMetadata(String name, FieldMetadataBuilder builder) {
		builder.field( "AddressClassBridge", FieldType.STRING );
	}
}
