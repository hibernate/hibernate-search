/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 *  Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 *  indicated by the @author tags or express copyright attribution
 *  statements applied by the authors.  All third-party contributions are
 *  distributed under license by Red Hat, Inc.
 *
 *  This copyrighted material is made available to anyone wishing to use, modify,
 *  copy, or redistribute it subject to the terms and conditions of the GNU
 *  Lesser General Public License, as published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 *  for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this distribution; if not, write to:
 *  Free Software Foundation, Inc.
 *  51 Franklin Street, Fifth Floor
 *  Boston, MA  02110-1301  USA
 */
package org.hibernate.search.engine;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

/**
 * Wrapper class for the loading of a single entity.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public class EntityInfo {
	/**
	 * The entity class.
	 */
	public final Class clazz;

	/**
	 * The document id.
	 */
	public final Serializable id;

	/**
	 * The name of the document id property.
	 */
	public final String idName;

	/**
	 * Array of projected values. {@code null} in case there are no projections.
	 */
	public final Object[] projection;

	public final List<Integer> indexesOfThis = new LinkedList<Integer>();

	public EntityInfo(Class clazz,  String idName,  Serializable id, Object[] projection) {
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
