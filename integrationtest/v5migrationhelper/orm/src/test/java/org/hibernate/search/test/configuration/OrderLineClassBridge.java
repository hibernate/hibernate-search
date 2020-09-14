/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.configuration;

import java.util.Map;

import org.apache.lucene.document.Document;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.MetadataProvidingFieldBridge;
import org.hibernate.search.bridge.ParameterizedBridge;
import org.hibernate.search.bridge.spi.FieldMetadataBuilder;
import org.hibernate.search.bridge.spi.FieldType;

/**
 * A class bridge which is used via the configuration API.
 *
 * @author Gunnar Morling
 */
public class OrderLineClassBridge implements MetadataProvidingFieldBridge, ParameterizedBridge {

	private String fieldName;

	public OrderLineClassBridge(String fieldName) {
		this.fieldName = fieldName;
	}

	@Override
	public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
		OrderLine orderLine = (OrderLine) value;
		luceneOptions.addFieldToDocument( fieldName != null ? fieldName : name, orderLine.getName(), document );
	}

	@Override
	public void setParameterValues(Map<String, String> parameters) {
		this.fieldName = parameters.get( "fieldName" );
	}

	@Override
	public void configureFieldMetadata(String name, FieldMetadataBuilder builder) {
		builder.field( fieldName != null ? fieldName : name, FieldType.STRING );
	}
}
