/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.engine;

import org.hibernate.search.ProjectionConstants;

/**
 * Interface created to keep backwards compatibility.
 *
 * @author Hardy Ferentschik
 */
public interface DocumentBuilder {

	/**
	 * Lucene document field name containing the fully qualified classname of the indexed class.
	 *
	 */
	String CLASS_FIELDNAME = ProjectionConstants.OBJECT_CLASS;

	/**
	 * The DocumentBuilder might be able to tell if an object state update is going to affect index state,
	 * so that if this function returns false we can skip updating the Lucene index.
	 * @since 3.4
	 * @param dirtyPropertyNames Contains the property name of each value which changed, or null for everything.
	 * @return true if it can't make sure the index doesn't need an update
	 */
	boolean isDirty(String[] dirtyPropertyNames);
}
