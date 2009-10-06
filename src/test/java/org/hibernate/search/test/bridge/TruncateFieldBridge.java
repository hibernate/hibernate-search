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
package org.hibernate.search.test.bridge;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.util.StringHelper;

/**
 * @author Emmanuel Bernard
 */
public class TruncateFieldBridge implements FieldBridge {
	public Object get(String name, Document document) {
		Field field = document.getField( name );
		return field.stringValue();
	}

	public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
		String indexedString = (String) value;
		//Do not add fields on empty strings, seems a sensible default in most situations
		if ( StringHelper.isNotEmpty( indexedString ) ) {
			Field field = new Field(name, indexedString.substring(0,
					indexedString.length() / 2), luceneOptions.getStore(),
					luceneOptions.getIndex(), luceneOptions.getTermVector());
			field.setBoost( luceneOptions.getBoost() );
			document.add( field );
		}
	}
}
