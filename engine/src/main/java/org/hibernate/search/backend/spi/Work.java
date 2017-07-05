/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.spi;

import java.io.Serializable;

import org.hibernate.search.spi.IndexedTypeIdentifier;

/**
 * A unit of work. Only make sense inside the same session since it uses the scope principle.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public class Work {
	private final Object entity;
	private final IndexedTypeIdentifier entityTypeId;
	private final Serializable id;
	private final WorkType type;
	private final boolean identifierWasRolledBack;
	private final String tenantIdentifier;

	public Work(Object entity, Serializable id, WorkType type) {
		this( null, entity, (IndexedTypeIdentifier) null, id, type, false );
	}

	public Work(Object entity, Serializable id, WorkType type, boolean identifierRollbackEnabled) {
		this( null, entity, (IndexedTypeIdentifier) null, id, type, identifierRollbackEnabled );
	}

	public Work(IndexedTypeIdentifier entityType, Serializable id, WorkType type) {
		this( null, null, entityType, id, type, false );
	}

	public Work(Object entity, WorkType type) {
		this( null, entity, (IndexedTypeIdentifier) null, null, type, false );
	}

	public Work(String tenantId, Object entity, Serializable id, WorkType type) {
		this( tenantId, entity, (IndexedTypeIdentifier) null, id, type, false );
	}

	public Work(String tenantId, Object entity, Serializable id, WorkType type, boolean identifierRollbackEnabled) {
		this( tenantId, entity, (IndexedTypeIdentifier) null, id, type, identifierRollbackEnabled );
	}

	public Work(String tenantId, IndexedTypeIdentifier entityType, Serializable id, WorkType type) {
		this( tenantId, null, entityType, id, type, false );
	}

	public Work(String tenantId, Object entity, WorkType type) {
		this( tenantId, entity, (IndexedTypeIdentifier) null, null, type, false );
	}

	private Work(String tenantId, Object entity, IndexedTypeIdentifier entityTypeId, Serializable id, WorkType type, boolean identifierWasRolledBack) {
		this.entity = entity;
		this.entityTypeId = entityTypeId;
		this.id = id;
		this.type = type;
		this.identifierWasRolledBack = identifierWasRolledBack;
		this.tenantIdentifier = tenantId;
	}

	public IndexedTypeIdentifier getTypeIdentifier() {
		return entityTypeId;
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
		sb.append( "entityTypeId=" ).append( entityTypeId );
		sb.append( ", tenantId=" ).append( tenantIdentifier );
		sb.append( ", id=" ).append( id );
		sb.append( ", type=" ).append( type );
		sb.append( '}' );
		return sb.toString();
	}
}
