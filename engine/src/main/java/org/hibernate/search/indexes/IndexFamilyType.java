/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.indexes;

/**
 * Represents a type of {@link IndexFamily}, to be used as a key to retrieve an actual instance of an index family.
 * <p>
 * The Lucene index family type can be retrieved from {@link LuceneEmbeddedIndexFamilyType#get()}.
 * <p>
 * The Elasticsearch index family type can be retrieved from
 * {@code org.hibernate.search.elasticsearch.indexes.ElasticsearchIndexFamilyType#get()}
 * (requires the Elasticsearch integration JAR).
 *
 * @hsearch.experimental This type is under active development as part of the Elasticsearch integration.
 *    You should be prepared for incompatible changes in future releases.
 */
public interface IndexFamilyType {
}
