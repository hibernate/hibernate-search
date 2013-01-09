/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
package org.hibernate.search;

import java.io.Serializable;

import org.hibernate.Session;
import org.hibernate.search.indexes.interceptor.EntityIndexingInterceptor;

/**
 * Extends the Hibernate {@link Session} with fulltext search and indexing capabilities.
 *
 * @author Emmanuel Bernard
 */
public interface FullTextSession extends Session {
	
	/**
	 * Create a fulltext query on top of a native Lucene query returning the matching objects
	 * of type <code>entities</code> and their respective subclasses.
	 *
	 * @param luceneQuery The native Lucene query to be run against the Lucene index.
	 * @param entities List of classes for type filtering. The query result will only return entities of
	 * the specified types and their respective subtype. If no class is specified no type filtering will take place.
	 *
	 * @return A <code>FullTextQuery</code> wrapping around the native Lucene query.
	 *
	 * @throws IllegalArgumentException if entityType is <code>null</code> or not a class or superclass annotated with <code>@Indexed</code>.
	 */
	FullTextQuery createFullTextQuery(org.apache.lucene.search.Query luceneQuery, Class<?>... entities);

	/**
	 * Force the (re)indexing of a given <b>managed</b> object.
	 * Indexation is batched per transaction: if a transaction is active, the operation
	 * will not affect the index at least until commit.
	 * 
	 * Any {@link EntityIndexingInterceptor} registered on the entity will be ignored:
	 * this method forces an index operation.
	 *
	 * @param entity The entity to index - must not be <code>null</code>.
	 *
	 * @throws IllegalArgumentException if entity is null or not an @Indexed entity
	 */
	<T> void index(T entity);

	/**
	 * @return the <code>SearchFactory</code> instance.
	 */
	SearchFactory getSearchFactory();

	/**
	 * Remove the entity with the type <code>entityType</code> and the identifier <code>id</code> from the index.
	 * If <code>id == null</code> all indexed entities of this type and its indexed subclasses are deleted. In this
	 * case this method behaves like {@link #purgeAll(Class)}.
	 *
	 * Any {@link EntityIndexingInterceptor} registered on the entity will be ignored:
	 * this method forces a purge operation.
	 *
	 * @param entityType The type of the entity to delete.
	 * @param id The id of the entity to delete.
	 *
	 * @throws IllegalArgumentException if entityType is <code>null</code> or not a class or superclass annotated with <code>@Indexed</code>.
	 */
	public <T> void purge(Class<T> entityType, Serializable id);

	/**
	 * Remove all entities from of particular class and all its subclasses from the index.
	 *
	 * Any {@link EntityIndexingInterceptor} registered on the entity type will be ignored.
	 *
	 * @param entityType The class of the entities to remove.
	 *
	 * @throws IllegalArgumentException if entityType is <code>null</code> or not a class or superclass annotated with <code>@Indexed</code>.
	 */
	public <T> void purgeAll(Class<T> entityType);

	/**
	 * Flush all index changes forcing Hibernate Search to apply all changes to the index not waiting for the batch limit.
	 */
	public void flushToIndexes();

	/**
	 * Creates a MassIndexer to rebuild the indexes of some
	 * or all indexed entity types.
	 * Instances cannot be reused.
	 *
	 * Any {@link EntityIndexingInterceptor} registered on the entity types are applied: each instance will trigger
	 * an {@link EntityIndexingInterceptor#onAdd(Object)} event from where you can customize the indexing operation.
	 *
	 * @param types optionally restrict the operation to selected types
	 * @return a new MassIndexer
	 */
	public MassIndexer createIndexer(Class<?>... types);

	/**
	 * {@inheritDoc}
	 */
	@Override
	public FullTextSharedSessionBuilder sessionWithOptions();

}
