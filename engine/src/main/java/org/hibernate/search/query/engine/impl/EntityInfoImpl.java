/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.engine.impl;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

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

	private final List<Integer> indexesOfThis = new LinkedList<Integer>();

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
	public List<Integer> getIndexesOfThis() {
		return indexesOfThis;
	}

	@Override
	public boolean isProjectThis() {
		return indexesOfThis.size() != 0;
	}

	@Override
	public void populateWithEntityInstance(Object entity) {
		for ( int index : indexesOfThis ) {
			projection[index] = entity;
		}
	}

	public EntityInfoImpl(Class clazz, String idName, Serializable id, Object[] projection) {
		this.clazz = clazz;
		this.idName = idName;
		this.id = id;
		if ( projection != null ) {
			this.projection = projection.clone();
		}
		else {
			this.projection = null;
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
