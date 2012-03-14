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
package org.hibernate.search.backend.spi;

import java.io.Serializable;

/**
 * A unit of work. Only make sense inside the same session since it uses the scope principle.
 *
 * @author Emmanuel Bernard
 */
public class Work<T> {
	private final T entity;
	private final Class<T> entityClass;
	private final Serializable id;
	private final WorkType type;
	private final boolean identifierWasRolledBack;
	
	public Work(T entity, Serializable id, WorkType type) {
		this( entity, null, id, type, false );
	}

	public Work(T entity, Serializable id, WorkType type, boolean identifierRollbackEnabled) {
		this( entity, null, id, type, identifierRollbackEnabled );
	}

	public Work(Class<T> entityType, Serializable id, WorkType type) {
		this( null, entityType, id, type, false );
	}

	public Work(T entity, WorkType type) {
		this( entity, null, null, type, false );
	}

	private Work(T entity, Class<T> entityClass, Serializable id,
				 WorkType type, boolean identifierWasRolledBack) {
		this.entity = entity;
		this.entityClass = entityClass;
		this.id = id;
		this.type = type;
		this.identifierWasRolledBack = identifierWasRolledBack;
	}

	public Class<T> getEntityClass() {
		return entityClass;
	}

	public T getEntity() {
		return entity;
	}

	public Serializable getId() {
		return id;
	}

	public WorkType getType() {
		return type;
	}

	public boolean isIdentifierWasRolledBack() {
		return identifierWasRolledBack;
	}
}
