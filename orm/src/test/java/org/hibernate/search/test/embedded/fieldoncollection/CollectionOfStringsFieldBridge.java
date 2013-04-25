/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
package org.hibernate.search.test.embedded.fieldoncollection;

import java.util.Collection;

import org.apache.lucene.document.Document;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.StringBridge;

public class CollectionOfStringsFieldBridge implements FieldBridge, StringBridge {

	@Override
	public String objectToString(Object object) {
		if ( object == null ) {
			return null;
		}
		if ( !( object instanceof String ) ) {
			throw new IllegalArgumentException( "This FieldBridge only supports String objects." );
		}
		return object.toString();
	}

	@Override
	public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
		if ( value == null ) {
			return;
		}
		if ( !( value instanceof Collection ) ) {
			throw new IllegalArgumentException( "This FieldBridge only supports collections." );
		}
		Collection<?> objects = (Collection<?>) value;

		for ( Object object : objects ) {
			luceneOptions.addFieldToDocument( name, objectToString( object ), document );
		}
	}
}
