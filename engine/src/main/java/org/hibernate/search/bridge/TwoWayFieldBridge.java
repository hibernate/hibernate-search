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

import org.apache.lucene.document.Document;

/**
 * A <code>FieldBridge</code> able to convert the index representation back into an object without losing information.
 * Any bridge expected to process a document id should implement this interface.
 *
 * @author Emmanuel Bernard
 */
// FIXME rework the interface inheritance there are some common concepts with StringBridge
public interface TwoWayFieldBridge extends FieldBridge {
	/**
	 * Build the element object from the <code>Document</code>
	 *
	 * @param name field name
	 * @param document document
	 * @return The return value is the entity property value.
	 */
	Object get(String name, Document document);

	/**
	 * Convert the object representation to a string.
	 *
	 * @param object The object to index.
	 * @return string (index) representation of the specified object. Must not be <code>null</code>, but
	 *         can be empty.
	 */
	String objectToString(Object object);
}
