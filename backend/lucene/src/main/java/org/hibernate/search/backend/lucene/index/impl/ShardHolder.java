/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.index.impl;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;

import org.hibernate.search.backend.lucene.cfg.LuceneIndexSettings;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexModel;
import org.hibernate.search.backend.lucene.lowlevel.reader.impl.ReadIndexManagerContext;
import org.hibernate.search.backend.lucene.lowlevel.reader.spi.IndexReaderHolder;
import org.hibernate.search.backend.lucene.orchestration.impl.LuceneWriteWorkOrchestrator;
import org.hibernate.search.backend.lucene.work.execution.impl.WorkExecutionIndexManagerContext;
import org.hibernate.search.engine.backend.index.spi.IndexManagerStartContext;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySource;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.impl.SuppressingCloser;

class ShardHolder implements Closeable, ReadIndexManagerContext, WorkExecutionIndexManagerContext {

	private static final ConfigurationProperty<Integer> NUMBER_OF_SHARDS =
			ConfigurationProperty.forKey( LuceneIndexSettings.ShardingRadicals.NUMBER_OF_SHARDS )
					.asInteger()
					.withDefault( LuceneIndexSettings.Defaults.SHARDING_NUMBER_OF_SHARDS )
					.build();

	private final IndexManagerBackendContext backendContext;
	private final LuceneIndexModel model;
	private final List<Shard> shards = new ArrayList<>();
	private final List<LuceneWriteWorkOrchestrator> writeOrchestrators = new ArrayList<>();

	ShardHolder(IndexManagerBackendContext backendContext, LuceneIndexModel model) {
		this.backendContext = backendContext;
		this.model = model;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[indexName=" + model.getIndexName() + "]";
	}

	void start(IndexManagerStartContext startContext) {
		ConfigurationPropertySource propertySource = startContext.getConfigurationPropertySource()
				.withMask( "sharding" );

		int shardSize = NUMBER_OF_SHARDS.get( propertySource );

		try {
			if ( shardSize == 1 ) {
				Shard shard = Shard.create( backendContext, model, OptionalInt.empty() );
				shards.add( shard );
			}
			else {
				for ( int i = 0; i < shardSize; i++ ) {
					Shard shard = Shard.create( backendContext, model, OptionalInt.of( i ) );
					shards.add( shard );
				}
			}
			for ( Shard shard : shards ) {
				writeOrchestrators.add( shard.getWriteOrchestrator() );
			}
		}
		catch (RuntimeException e) {
			new SuppressingCloser( e )
					.pushAll( shards );
			shards.clear();
			writeOrchestrators.clear();
			throw e;
		}
	}

	@Override
	public void close() throws IOException {
		try ( Closer<IOException> closer = new Closer<>() ) {
			closer.pushAll( Shard::close, shards );
			shards.clear();
			writeOrchestrators.clear();
		}
	}

	@Override
	public String getIndexName() {
		return model.getIndexName();
	}

	@Override
	public void openIndexReaders(Set<String> routingKeys, Collection<IndexReaderHolder> readerCollector)
			throws IOException {
		BitSet enabledShards = new BitSet( shards.size() );
		if ( routingKeys.isEmpty() ) {
			// No routing key => target all shards
			enabledShards.set( 0, shards.size() );
		}
		else {
			for ( String routingKey : routingKeys ) {
				int shardId = toShardId( routingKey );
				enabledShards.set( shardId );
			}
		}

		for ( int i = 0; i < shards.size(); i++ ) {
			if ( enabledShards.get( i ) ) {
				readerCollector.add( shards.get( i ).openReader() );
			}
		}
	}

	@Override
	public LuceneWriteWorkOrchestrator getWriteOrchestrator(String documentId, String routingKey) {
		if ( routingKey == null ) {
			routingKey = documentId;
		}
		return shards.get( toShardId( routingKey ) ).getWriteOrchestrator();
	}

	@Override
	public Collection<LuceneWriteWorkOrchestrator> getAllWriteOrchestrators() {
		return writeOrchestrators;
	}

	private int toShardId(String routingKey) {
		int shardSize = shards.size();
		if ( shardSize == 1 ) {
			// Don't bother hashing: sharding is disabled.
			return 0;
		}

		return Math.abs( hash( routingKey ) % shards.size() );
	}

	private static int hash(String routingKey) {
		if ( routingKey == null ) {
			return 0;
		}

		// reproduce the hashCode implementation of String as documented in the javadoc
		// to be safe cross Java version (in case it changes some day)
		int hash = 0;
		int length = routingKey.length();
		for ( int index = 0; index < length; index++ ) {
			hash = 31 * hash + routingKey.charAt( index );
		}
		return hash;
	}

	public List<Shard> getShardsForTests() {
		return new ArrayList<>( shards );
	}
}
