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

import java.io.Serializable;
import java.util.Properties;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.hibernate.search.filter.FullTextFilterImplementor;
import org.hibernate.search.spi.BuildContext;

/**
 * Implementations provide the identifiers of those shards to be taken into account by the engine when working with
 * specified entities or queries.
 * <p>
 * Implementation notes:
 * <p>
 * With exception of the {@link ShardIdentifierProvider#initialize(Properties, BuildContext)} method which is invoked
 * only once at startup, all other methods could be invoked in parallel by independent threads; implementations must
 * thus be thread-safe.
 * <p>
 * Instead of implementing this interface directly, implementations should be derived from
 * {@link ShardIdentifierProviderTemplate} as new methods might be added to this interface in future releases.
 *
 * @experimental The exact method signatures are likely to change in future.
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 * @author Hardy Ferentschik
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2013 Red Hat Inc.
 */
public interface ShardIdentifierProvider {

	/**
	 * Initialize this provider.
	 * <br/>
	 * This method is invoked only once per instance and before any other method is invoked.
	 *
	 * @param properties The configuration properties
	 * @param buildContext The build context available during bootstrapping
	 */
	void initialize(Properties properties, BuildContext buildContext);

	/**
	 * Determine the shard identifier for the given entity.
	 * <br/>
	 * Note: Implementations will usually inspect a specific fieldable of the document in order to determine the shard
	 * identifier, for example a customer id or a language code.
	 * <br/>
	 * Concurrency: this method could be invoked concurrently. That means you could have multiple invocations of
	 * {@link #getShardIdentifier(Class, Serializable, String, Document)}, {@link #getShardIdentifiersForQuery(FullTextFilterImplementor[])},
	 * {@link #getAllShardIdentifiers()}.
	 *
	 * @param entityType the type of the entity
	 * @param id the id of the entity
	 * @param idAsString the entity id transformed as string via the appropriate document id bridge
	 * @param document the Lucene document for the entity with the given id
	 *
	 * @return the shard identifier to which the entity specified by the given parameters belongs to.
	 */
	String getShardIdentifier(Class<?> entityType, Serializable id, String idAsString, Document document);

	/**
	 * Returns the set of shard identifiers for a query given the applied filters.
	 *
	 * The method allows to limit the shards a given query targets depending on the selected filters.
	 * <br/>
	 * Concurrency: this method could be invoked concurrently. That means you could have multiple invocations of
	 * {@link #getShardIdentifier(Class, Serializable, String, Document)}, {@link #getShardIdentifiersForQuery(FullTextFilterImplementor[])},
	 * {@link #getAllShardIdentifiers()}.
	 *
	 * @param fullTextFilters the filters which are applied to the current query
	 *
	 * @return the set of shard identifiers this query should target
	 */
	Set<String> getShardIdentifiersForQuery(FullTextFilterImplementor[] fullTextFilters);

	/**
	 * Returns the list of all currently known shard identifiers.
	 * <br/>
	 * Note: The list can vary between calls!
	 * <br/>
	 * Concurrency: this method could be invoked concurrently. That means you could have multiple invocations of
	 * {@link #getShardIdentifier(Class, Serializable, String, Document)}, {@link #getShardIdentifiersForQuery(FullTextFilterImplementor[])},
	 * {@link #getAllShardIdentifiers()}.
	 *
	 * @return the set of all currently known shard identifiers.
	 */
	Set<String> getAllShardIdentifiers();
}
