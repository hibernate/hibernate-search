/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.metadata.impl;

import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.metadata.IndexDescriptor;

/**
 * @author Hardy Ferentschik
 */
public class IndexDescriptorImpl implements IndexDescriptor {
	private final String indexName;

	// TODO - HSEARCH-436 fix constructor arguments
	public IndexDescriptorImpl(IndexManager[] indexManagers) {
		indexName = indexManagers[0].getIndexName();
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


