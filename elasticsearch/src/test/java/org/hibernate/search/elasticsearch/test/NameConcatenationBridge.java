/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.test;

import org.apache.lucene.document.Document;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.MetadataProvidingFieldBridge;
import org.hibernate.search.bridge.spi.FieldMetadataBuilder;
import org.hibernate.search.bridge.spi.FieldType;

/**
 * @author Gunnar Morling
 */
public class NameConcatenationBridge implements MetadataProvidingFieldBridge {

	@Override
	public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
		GolfPlayer player = (GolfPlayer) value;

		StringBuilder names = new StringBuilder();
		if ( player.getFirstName() != null ) {
			names.append( player.getFirstName() ).append( " " );
		}
		if ( player.getLastName() != null ) {
			names.append( player.getLastName() );
		}

		luceneOptions.addFieldToDocument( name, names.toString(), document );
	}

	@Override
	public void configureFieldMetadata(String name, FieldMetadataBuilder builder) {
		builder.field( name, FieldType.STRING );
	}
}
