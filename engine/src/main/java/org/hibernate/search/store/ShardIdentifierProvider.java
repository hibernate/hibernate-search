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
 * Provides shard identifiers when dynamic sharding is used.
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 * @author Hardy Ferentschik
 */
public interface ShardIdentifierProvider {

	/**
	 * Initialize this provider.
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
	 *
	 * @return the list of all currently known shard identifiers.
	 */
	Set<String> getAllShardIdentifiers();
}
