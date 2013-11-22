/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.search.indexes.interceptor;

/**
 * This interceptor is called upon indexing operations to optionally change the behavior.
 * Implementations must be thread safe and should have a no-arg constructor.
 *
 * Typical use case include so called soft deletes.
 *
 * The interceptor is applied to a MassIndexer operation, but is ignored when using
 * the explicit indexing control API such <code>org.hibernate.search.FullTextSession.index(T)</code>
 * or <code>purge</code>, <purgeAll>.
 *
 * @experimental {@link IndexingOverride} might be updated
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public interface EntityIndexingInterceptor<T> {

	/**
	 * Triggered when an entity instance T should be added to the index, either by an event listener or by the
	 * MassIndexer.
	 * This is not triggered by an explicit API call such as FullTextSession.index(T).
	 *
	 * @param entity
	 *            The entity instance
	 * @return Return {@link IndexingOverride#APPLY_DEFAULT} to have the instance be added to the index as normal; return a
	 *         different value to override the behaviour.
	 */
	IndexingOverride onAdd(T entity);

	/**
	 * Triggered when an entity instance T should be updated in the index.
	 *
	 * @param entity
	 *            The entity instance
	 * @return Return {@link IndexingOverride#APPLY_DEFAULT} to have the instance removed and re-added to the index as
	 *         normal; return a different value to override the behaviour.
	 */
	IndexingOverride onUpdate(T entity);

	/**
	 * Triggered when an entity instance T should be deleted from the index.
	 *
	 * @param entity
	 *            The entity instance
	 * @return Return {@link IndexingOverride#APPLY_DEFAULT} to have the instance removed from the index as normal;
	 *         return a different value to override the behaviour.
	 */
	IndexingOverride onDelete(T entity);

	/**
	 * A CollectionUpdate event is fired on collections included in an indexed entity, for example when using
	 * {@link org.hibernate.search.annotations.IndexedEmbedded} This event is triggered on each indexed domain instance T contained in such a collection;
	 * this is generally similar to a {@link #onUpdate(Object)} event.
	 *
	 * @param entity The entity instance
	 *
	 * @return Return {@link IndexingOverride#APPLY_DEFAULT} to have the instance removed and re-added to the index as
	 *         normal; return a different value to override the behaviour.
	 */
	IndexingOverride onCollectionUpdate(T entity);

}
