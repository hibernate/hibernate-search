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
package org.hibernate.search.backend;

import java.io.Serializable;

import org.hibernate.search.backend.impl.WorkVisitor;

/**
 * A unit of work triggering an optimize operation.
 * This work does not propagate to a cluster: it should be filtered before being sent to
 * the network.
 *
 * @author Andrew Hahn
 * @author Emmanuel Bernard
 */
public class OptimizeLuceneWork extends LuceneWork implements Serializable {

	public static final OptimizeLuceneWork INSTANCE = new OptimizeLuceneWork();

	/**
	 * Optimizes the index(es) of a specific entity
	 * @param entity
	 */
	public OptimizeLuceneWork(Class<?> entity) {
		super( null, null, entity );
	}

	/**
	 * Optimizes any index
	 */
	private OptimizeLuceneWork() {
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
			return "OptimizeLuceneWork: global";
		}
		else {
			return "OptimizeLuceneWork: " + this.getEntityClass().getName();
		}
	}

}
