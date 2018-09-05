/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.metadata.impl;

import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.metadata.IndexDescriptor;

/**
 * @author Hardy Ferentschik
 */
public class IndexDescriptorImpl implements IndexDescriptor {
	private final String indexName;

	public IndexDescriptorImpl(IndexManager indexManager) {
		indexName = indexManager.getIndexName();
	}

	@Override
	public String getName() {
		return indexName;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder( "IndexDescriptorImpl{" );
		sb.append( "indexName='" ).append( indexName ).append( '\'' );
		sb.append( '}' );
		return sb.toString();
	}
}


