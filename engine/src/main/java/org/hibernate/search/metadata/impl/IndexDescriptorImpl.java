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

import java.util.HashSet;
import java.util.Set;

import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.metadata.IndexDescriptor;

/**
 * @author Hardy Ferentschik
 */
public class IndexDescriptorImpl implements IndexDescriptor {
	private final boolean sharded;
	private final Set<String> indexNames;
	private final String primaryIndexName;

	public IndexDescriptorImpl(IndexManager[] indexManagers) {
		primaryIndexName = indexManagers[0].getIndexName();
		sharded = indexManagers.length > 1;
		indexNames = new HashSet<String>();
		for ( IndexManager indexManager : indexManagers ) {
			indexNames.add( indexManager.getIndexName() );
		}
	}

	@Override
	public String getName() {
		return primaryIndexName;
	}

	@Override
	public boolean isSharded() {
		return sharded;
	}

	@Override
	public Set<String> getShardNames() {
		return indexNames;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder( "IndexDescriptorImpl{" );
		sb.append( "sharded=" ).append( sharded );
		sb.append( ", indexNames=" ).append( indexNames );
		sb.append( ", primaryIndexName='" ).append( primaryIndexName ).append( '\'' );
		sb.append( '}' );
		return sb.toString();
	}
}


