/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.integration.tika.model;

import org.apache.lucene.document.Document;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.XMPDM;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.TikaMetadataProcessor;

/**
 * @author Davide D'Alto
 */
public class Mp3TikaMetadataProcessor implements TikaMetadataProcessor {

	@Override
	public Metadata prepareMetadata() {
		return new Metadata();
	}

	@Override
	public void set(String name, Object value, Document document, LuceneOptions luceneOptions, Metadata metadata) {
		luceneOptions.addFieldToDocument( XMPDM.ARTIST.getName(), metadata.get( XMPDM.ARTIST ), document );
	}
}
