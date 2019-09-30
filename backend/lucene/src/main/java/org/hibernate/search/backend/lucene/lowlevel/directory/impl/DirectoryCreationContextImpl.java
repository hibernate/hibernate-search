/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.directory.impl;

import java.lang.invoke.MethodHandles;
import java.util.Optional;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.lowlevel.directory.spi.DirectoryCreationContext;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reporting.EventContext;

/**
 * The implementation of {@link DirectoryCreationContext}.
 *
 * @author Emmanuel Bernard
 * @author Sanne Grinovero
 * @author Hardy Ferentschik
 * @author Gunnar Morling
 */
public class DirectoryCreationContextImpl implements DirectoryCreationContext {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final EventContext eventContext;
	private final String indexName;
	private final Optional<String> shardId;

	public DirectoryCreationContextImpl(EventContext eventContext, String indexName, Optional<String> shardId) {
		this.eventContext = eventContext;
		this.indexName = indexName;
		this.shardId = shardId;
	}

	@Override
	public EventContext getEventContext() {
		return eventContext;
	}

	@Override
	public String getIndexName() {
		return indexName;
	}

	@Override
	public Optional<String> getShardId() {
		return shardId;
	}

}
