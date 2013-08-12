/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
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
}
