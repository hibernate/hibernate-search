/* $Id$
 * 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * Copyright (c) 2009, Red Hat, Inc. and/or its affiliates or third-party contributors as
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

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Emmanuel Bernard
 */
public class EntityInfo {
	
	public final Class clazz;
	public final Serializable id;
	public final Object[] projection;
	public final List<Integer> indexesOfThis = new LinkedList<Integer>();
	
	public EntityInfo(Class clazz, Serializable id, Object[] projection) {
		this.clazz = clazz;
		this.id = id;
		this.projection = projection;
	}
	
}
