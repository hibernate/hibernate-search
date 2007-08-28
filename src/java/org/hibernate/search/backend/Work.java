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
	private Object entity;
	private Class entityClass;
	private Serializable id;
	private XMember idGetter;
	private WorkType type;


	public Work(Object entity, Serializable id, WorkType type) {
		this.entity = entity;
		this.id = id;
		this.type = type;
	}

	public Work(Class entityType, Serializable id, WorkType type) {
		this.entityClass = entityType;
		this.id = id;
		this.type = type;
	}

	public Work(Object entity, XMember idGetter, WorkType type) {
		this.entity = entity;
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
