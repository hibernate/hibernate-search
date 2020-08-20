/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.bridge;

import org.apache.lucene.document.Document;
import org.apache.tika.metadata.Metadata;

/**
 * @author Hardy Ferentschik
 */
public interface TikaMetadataProcessor {
	/**
	 * This method is called by the {@link org.hibernate.search.bridge.builtin.TikaBridge} prior to processing the data
	 *
	 * @return Tika metadata used for data processing. Additional metadata can be set here.
	 * @see <a href="http://tika.apache.org/1.1/parser.html#apiorgapachetikametadataMetadata.html">Tika API</a>
	 */
	Metadata prepareMetadata();

	/**
	 * This method called by the {@link org.hibernate.search.bridge.builtin.TikaBridge} after processing the data.
	 * It can be used to add extracted metadata to the  document.
	 *
	 * @param name The field name  to add to the Lucene document
	 * @param value The value to index
	 * @param document The Lucene document into which we want to index the value.
	 * @param luceneOptions Contains the parameters used for adding {@code value} to
	 * the Lucene document.
	 * @param metadata the metadata discovered by the Tika parsing process
	 */
	void set(String name, Object value, Document document, LuceneOptions luceneOptions, Metadata metadata);
}
