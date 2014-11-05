/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
 * @hsearch.experimental The exact method signatures are likely to change in future.
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
