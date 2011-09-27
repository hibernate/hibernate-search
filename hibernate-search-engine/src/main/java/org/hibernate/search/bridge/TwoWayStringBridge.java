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
 * <code>StringBridge<code> allowing a translation from the string representation back to the <code>Object</code>.
 * <code>objectToString( stringToObject( string ) )</code> and <code>stringToObject( objectToString( object ) )</code>
 * should be "idempotent". More precisely:
 * <ul>
 * <li><code>objectToString( stringToObject( string ) ).equals(string)</code>, for non <code>null</code> string.</li>
 * <li><code>stringToObject( objectToString( object ) ).equals(object)</code>, for non <code>null</code> object. </li>
 * </ul>
 * 
 * As for all Bridges implementations must be threadsafe.
 * 
 * @author Emmanuel Bernard
 */
public interface TwoWayStringBridge extends StringBridge {

	/**
	 * Convert the index string representation to an object.
	 *
	 * @param stringValue The index value.
	 * @return Takes the string representation from the Lucene index and transforms it back into the original
	 * <code>Object</code>.
	 */
	Object stringToObject(String stringValue);
}
