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
package org.hibernate.search.interceptor.indexingaction;

/**
 * This interceptor is called upon indexing action to optionally change the behavior.
 * Implementations must be thread safe and should have a no-arg constructor.
 *
 * Typical use case include so called soft deletes.
 *
 * Experimental: IndexingActionTypes might be updated
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public interface IndexingActionInterceptor<T> {
	IndexingActionType onAdd(T entity);
	IndexingActionType onUpdate(T entity);
	IndexingActionType onDelete(T entity);
	IndexingActionType onCollectionUpdate(T entity);

	//FIXME should we add onPurge and onIndex?
}
