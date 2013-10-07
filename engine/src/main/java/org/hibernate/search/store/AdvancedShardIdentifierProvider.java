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
package org.hibernate.search.store;

import java.io.Serializable;
import java.util.Set;

/**
 * Normally a sharding strategy is defined by creating a {@link ShardIdentifierProvider} implementation.
 *
 * This interface represents a more advanced alternative available to get slightly better performance in
 * the case your sharding strategy is able to narrow down the shards which need to be cleaned up during a
 * delete or purge operation. If you implement just {@link ShardIdentifierProvider} contract a delete
 * operation will always trigger a write on each shard; by implementing {@link AdvancedShardIdentifierProvider}
 * you can narrow this down to a subset of shards, ideally but not necessarily to just one or even none.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public interface AdvancedShardIdentifierProvider extends ShardIdentifierProvider {

	/**
	 * Determine the shard identifiers of indexes which might contain an entity identified solely by its id.
	 * This is needed for purge and delete operations, as no more context is available in such cases.
	 * <br/>
	 * This method is made available as some strategies might be able to provide a deterministic answer, but in
	 * most cases you can safely return the same set as returned by {@link #getAllShardIdentifiers()}.
	 * <br/>
	 * Concurrency: this method could be invoked concurrently. That means you could have multiple invocations of
	 * {@link #getShardIdentifier(Class, Serializable, String, Document)}, {@link #getShardIdentifiersForQuery(FullTextFilterImplementor[])},
	 * {@link #getAllShardIdentifiers()}, {@link #getShardIdentifiersForDeletion(Class, Serializable, String)}.
	 *
	 * @param entityType the type of the entity
	 * @param id the id of the entity
	 * @param idAsString the entity id transformed as string via the appropriate document id bridge
	 *
	 * @return the set of shards which might contain the document, narrowing down from the given parameters.
	 */
	Set<String> getShardIdentifiersForDeletion(Class<?> entityType, Serializable id, String idAsString);

}
