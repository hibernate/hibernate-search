/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.metamodel;

import org.hibernate.search.engine.backend.metamodel.IndexDescriptor;

/**
 * A descriptor of an Elasticsearch backend index,
 * which exposes additional information specific to this backend.
 */
public interface ElasticsearchIndexDescriptor extends IndexDescriptor {

	/**
	 * @return The read name,
	 * i.e. the name that Hibernate Search is supposed to use when executing searches on the index.
	 */
	String readName();

	/**
	 * @return The write name,
	 * i.e. the name that Hibernate Search is supposed to use when indexing or purging the index.
	 */
	String writeName();

}
