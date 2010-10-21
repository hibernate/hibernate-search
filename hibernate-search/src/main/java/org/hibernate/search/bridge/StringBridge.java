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
package org.hibernate.search.bridge;

/**
 * Transform an object into a string representation.
 * 
 * All implementations are required to be threadsafe.
 * Usually this is easily achieved avoiding the usage
 * of class fields, unless they are either immutable
 * or needed to store parameters.
 *
 * @author Emmanuel Bernard
 */
public interface StringBridge {
	
	/**
	 * Converts the object representation to a string.
	 *
	 * @param object The object to transform into a string representation.
	 * @return String representation of the given object to be stored in Lucene index. The return string must not be
	 * <code>null</code>. It can be empty though.
	 */
	String objectToString(Object object);
}
