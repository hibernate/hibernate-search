/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.spi;

import java.io.Serializable;

/**
 * A unit of work. Only make sense inside the same session since it uses the scope principle.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
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

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder( "Work{" );
		sb.append( "entityClass=" ).append( entityClass );
		sb.append( ", id=" ).append( id );
		sb.append( ", type=" ).append( type );
		sb.append( '}' );
		return sb.toString();
	}
}
