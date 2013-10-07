/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.search.store;

import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.hibernate.search.filter.FullTextFilterImplementor;
import org.hibernate.search.spi.BuildContext;

/**
 * Recommended parent class to create custom {@link ShardIdentifierProvider} implementations. Sub-classes must provide a
 * no-arg constructor.
 *
 * @experimental The exact method signatures are likely to change in future.
 *
 * @author Sanne Grinovero
 */
public abstract class ShardIdentifierProviderTemplate implements ShardIdentifierProvider {

	private volatile Set<String> knownShards = Collections.emptySet();

	@Override
	public final void initialize(Properties properties, BuildContext buildContext) {
		Set<String> initialShardNames = loadInitialShardNames( properties, buildContext );
		knownShards = Collections.unmodifiableSet( new HashSet<String>( initialShardNames ) );
	}

	protected abstract Set<String> loadInitialShardNames(Properties properties, BuildContext buildContext);

	protected final void addShard(final String shardName) {
		if ( ! knownShards.contains( shardName ) ) {
			addShardSynchronized( shardName );
		}
	}

	private synchronized void addShardSynchronized(final String shardName) {
		HashSet<String> newCopy = new HashSet<String>( knownShards );
		newCopy.add( shardName );
		knownShards = Collections.unmodifiableSet( newCopy );
	}

	@Override
	public final Set<String> getAllShardIdentifiers() {
		return knownShards;
	}

	/**
	 * Potentially suited to be overridden if you are able to narrow down the shard
	 * selection based on the active FullTextFilters.
	 */
	@Override
	public Set<String> getShardIdentifiersForQuery(FullTextFilterImplementor[] fullTextFilters) {
		return getAllShardIdentifiers();
	}

}
