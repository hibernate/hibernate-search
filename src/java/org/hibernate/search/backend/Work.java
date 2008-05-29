//$Id$
package org.hibernate.search.backend;

import java.io.Serializable;

import org.hibernate.annotations.common.reflection.XMember;

/**
 * work unit. Only make sense inside the same session since it uses the scope principle
 *
 * @author Emmanuel Bernard
 */
public class Work {
	private final Object entity;
	private final Class entityClass;
	private final Serializable id;
	private final XMember idGetter;
	private final WorkType type;

	public Work(Object entity, Serializable id, WorkType type) {
		this( entity, null, id, null, type );
	}

	public Work(Class entityType, Serializable id, WorkType type) {
		this( null, entityType, id, null, type );
	}

	public Work(Object entity, XMember idGetter, WorkType type) {
		this( entity, null, null, idGetter, type );
	}
	
	private Work(Object entity, Class entityClass, Serializable id,
			XMember idGetter, WorkType type) {
		this.entity = entity;
		this.entityClass = entityClass;
		this.id = id;
		this.idGetter = idGetter;
		this.type = type;
	}

	public Class getEntityClass() {
		return entityClass;
	}

	public Object getEntity() {
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
