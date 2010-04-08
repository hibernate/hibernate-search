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
package org.hibernate.search.test.compression;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.TwoWayFieldBridge;

/**
 * This FieldBridge is storing strings in the index wrapping the
 * entity value in html bold tags.
 * It's using deprecated method "getStore" to verify backwards compatibility
 * Almost useless, needed for CompressionTest
 * @see LuceneOptions
 * @author Sanne Grinovero
 */
public class HTMLBoldFieldBridge implements FieldBridge, TwoWayFieldBridge {

	public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
		String fieldValue = objectToString( value );
		Field field = new Field( name, fieldValue, luceneOptions.getStore(),
				luceneOptions.getIndex(), luceneOptions.getTermVector() );
		field.setBoost( luceneOptions.getBoost() );
		document.add( field );
	}

	public Object get(String name, Document document) {
		Field field = document.getField( name );
		String stringValue = field.stringValue();
		return stringValue.substring( 3, stringValue.length()-4 );
	}

	public String objectToString(Object value) {
		String originalValue = value.toString();
		String fieldValue = "<b>" + originalValue + "</b>";
		return fieldValue;
	}

}

