/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
package org.hibernate.search.backend;

import java.io.Serializable;

import org.hibernate.search.backend.impl.WorkVisitor;

/**
 * Used to flush and commit asynchronous and other pending operations on the Indexes.
 * Generally not needed, this is mainly used at the end of mass indexing operations.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2012 Red Hat Inc.
 * @since 4.1
 */
public class FlushLuceneWork extends LuceneWork implements Serializable {

	public static final FlushLuceneWork INSTANCE = new FlushLuceneWork();

	/**
	 * Flushes all index operations for a specific entity.
	 *
	 * @param entity the entity type for which to flush the index
	 */
	public FlushLuceneWork(Class<?> entity) {
		super( null, null, entity );
	}

	/**
	 * Flushes all index operations
	 */
	private FlushLuceneWork() {
		super( null, null, null );
	}

	@Override
	public <T> T getWorkDelegate(final WorkVisitor<T> visitor) {
		return visitor.getDelegate( this );
	}

	@Override
	public String toString() {
		Class entityClass = this.getEntityClass();
		if ( entityClass == null ) {
			return "FlushLuceneWork: global";
		}
		else {
			return "FlushLuceneWork: " + this.getEntityClass().getName();
		}
	}
}
