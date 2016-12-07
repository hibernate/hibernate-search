/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.spi;

import org.hibernate.search.analyzer.impl.RemoteAnalyzer;
import org.hibernate.search.analyzer.impl.RemoteAnalyzerProvider;
import org.hibernate.search.elasticsearch.nulls.impl.ElasticsearchMissingValueStrategy;
import org.hibernate.search.elasticsearch.nulls.impl.ElasticsearchNullMarkerIndexStrategy;
import org.hibernate.search.engine.nulls.impl.MissingValueStrategy;
import org.hibernate.search.indexes.spi.AnalyzerExecutionStrategy;
import org.hibernate.search.indexes.spi.IndexManagerType;

public final class ElasticsearchIndexManagerType implements IndexManagerType, RemoteAnalyzerProvider {

	public static final ElasticsearchIndexManagerType INSTANCE = new ElasticsearchIndexManagerType();

	private final ElasticsearchMissingValueStrategy missingValueStrategy =
			new ElasticsearchMissingValueStrategy( ElasticsearchNullMarkerIndexStrategy.DEFAULT );

	private final ElasticsearchMissingValueStrategy containerMissingValueStrategy =
			new ElasticsearchMissingValueStrategy( ElasticsearchNullMarkerIndexStrategy.CONTAINER );

	private ElasticsearchIndexManagerType() {
		//use the INSTANCE singleton
	}

	@Override
	public AnalyzerExecutionStrategy getAnalyzerExecutionStrategy() {
		return AnalyzerExecutionStrategy.REMOTE;
	}

	@Override
	public MissingValueStrategy getMissingValueStrategy() {
		return missingValueStrategy;
	}

	@Override
	public MissingValueStrategy getContainerMissingValueStrategy() {
		return containerMissingValueStrategy;
	}

	@Override
	public RemoteAnalyzer getRemoteAnalyzer(String name) {
		return new RemoteAnalyzer( name );
	}
}
