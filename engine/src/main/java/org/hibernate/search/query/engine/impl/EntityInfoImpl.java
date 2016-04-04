/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.engine.impl;

import java.io.Serializable;

import org.hibernate.search.engine.ProjectionConstants;
import org.hibernate.search.query.engine.spi.EntityInfo;

/**
 * Wrapper class describing the loading of an element.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public class EntityInfoImpl implements EntityInfo {
	/**
	 * The entity class.
	 */
	private final Class<?> clazz;

	/**
	 * The document id.
	 */
	private final Serializable id;

	/**
	 * The name of the document id property.
	 */
	private final String idName;

	/**
	 * Array of projected values. {@code null} in case there are no projections.
	 */
	private final Object[] projection;

	/**
	 * The entity instance returned in a search.
	 *
	 * @see ProjectionConstants#THIS
	 */
	private Object entityInstance;

	public EntityInfoImpl(Class clazz, String idName, Serializable id, Object[] projection) {
		this.clazz = clazz;
		this.idName = idName;
		this.id = id;
		this.projection = projection;
	}

	@Override
	public Class<?> getClazz() {
		return clazz;
	}

	@Override
	public Serializable getId() {
		return id;
	}

	@Override
	public String getIdName() {
		return idName;
	}

	@Override
	public Object[] getProjection() {
		return projection;
	}

	@Override
	public Object getEntityInstance() {
		return entityInstance;
	}

	@Override
	public void populateWithEntityInstance(Object entity) {
		this.entityInstance = entity;
		for ( int i = 0; i < projection.length; i++ ) {
			if ( projection[i] == ENTITY_PLACEHOLDER ) {
				projection[i] = entity;
			}
		}
	}

	@Override
	public String toString() {
		return "EntityInfoImpl{" +
				"idName='" + idName + '\'' +
				", id=" + id +
				", clazz=" + clazz +
				'}';
	}
}
