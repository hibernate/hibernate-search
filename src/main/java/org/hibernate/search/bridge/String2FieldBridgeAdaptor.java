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
package org.hibernate.search.bridge;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.hibernate.util.StringHelper;

/**
 * Bridge to use a StringBridge as a FieldBridge.
 *
 * @author Emmanuel Bernard
 */
public class String2FieldBridgeAdaptor implements FieldBridge {
	private final StringBridge stringBridge;

	public String2FieldBridgeAdaptor(StringBridge stringBridge) {
		this.stringBridge = stringBridge;
	}

	public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
		String indexedString = stringBridge.objectToString( value );
		//Do not add fields on empty strings, seems a sensible default in most situations
		//TODO if Store, probably also save empty ones
		if ( StringHelper.isNotEmpty( indexedString ) ) {
			Field field = new Field( name, indexedString, luceneOptions.getStore(), luceneOptions.getIndex(), luceneOptions.getTermVector() );
			field.setBoost( luceneOptions.getBoost() );
			document.add( field );
		}
	}

}
