//$Id$
package org.hibernate.search.backend;

import java.io.Serializable;

import org.hibernate.annotations.common.reflection.XMember;

/**
 * A unit of work. Only make sense inside the same session since it uses the scope principle.
 *
 * @author Emmanuel Bernard
 */
public class Work<T> {
	private final T entity;
	private final Class<T> entityClass;
	private final Serializable id;
	private final XMember idGetter;
	private final WorkType type;

	public Work(T entity, Serializable id, WorkType type) {
		this( entity, null, id, null, type );
	}

	public Work(Class<T> entityType, Serializable id, WorkType type) {
		this( null, entityType, id, null, type );
	}

	public Work(T entity, XMember idGetter, WorkType type) {
		this( entity, null, null, idGetter, type );
	}

	private Work(T entity, Class<T> entityClass, Serializable id,
				 XMember idGetter, WorkType type) {
		this.entity = entity;
		this.entityClass = entityClass;
		this.id = id;
		this.idGetter = idGetter;
		this.type = type;
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

	public XMember getIdGetter() {
		return idGetter;
	}

	public WorkType getType() {
		return type;
	}
}
