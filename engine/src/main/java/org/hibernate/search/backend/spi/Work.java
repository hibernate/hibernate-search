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
public class Work {
	private final Object entity;
	private final Class<?> entityClass;
	private final Serializable id;
	private final WorkType type;
	private final boolean identifierWasRolledBack;
	private final String tenantIdentifier;

	public Work(Object entity, Serializable id, WorkType type) {
		this( null, entity, null, id, type, false );
	}

	public Work(Object entity, Serializable id, WorkType type, boolean identifierRollbackEnabled) {
		this( null, entity, null, id, type, identifierRollbackEnabled );
	}

	public Work(Class<?> entityType, Serializable id, WorkType type) {
		this( null, null, entityType, id, type, false );
	}

	public Work(Object entity, WorkType type) {
		this( null, entity, null, null, type, false );
	}

	public Work(String tenantId, Object entity, Serializable id, WorkType type) {
		this( tenantId, entity, null, id, type, false );
	}

	public Work(String tenantId, Object entity, Serializable id, WorkType type, boolean identifierRollbackEnabled) {
		this( tenantId, entity, null, id, type, identifierRollbackEnabled );
	}

	public Work(String tenantId, Class<?> entityType, Serializable id, WorkType type) {
		this( tenantId, null, entityType, id, type, false );
	}

	public Work(String tenantId, Object entity, WorkType type) {
		this( tenantId, entity, null, null, type, false );
	}

	private Work(String tenantId, Object entity, Class<?> entityClass, Serializable id,
			WorkType type, boolean identifierWasRolledBack) {
		this.entity = entity;
		this.entityClass = entityClass;
		this.id = id;
		this.type = type;
		this.identifierWasRolledBack = identifierWasRolledBack;
		this.tenantIdentifier = tenantId;
	}

	public Class<?> getEntityClass() {
		return entityClass;
	}

	public String getTenantIdentifier() {
		return tenantIdentifier;
	}

	public Object getEntity() {
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
		sb.append( ", tenantId=" ).append( tenantIdentifier );
		sb.append( ", id=" ).append( id );
		sb.append( ", type=" ).append( type );
		sb.append( '}' );
		return sb.toString();
	}
}
