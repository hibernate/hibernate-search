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

import java.util.zip.DataFormatException;

import org.apache.lucene.document.CompressionTools;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.hibernate.search.SearchException;

/**
 * Bridge to use a TwoWayStringBridge as a TwoWayFieldBridge
 *
 * @author Emmanuel Bernard
 */
//TODO use Generics to avoid double declaration of stringBridge 
public class TwoWayString2FieldBridgeAdaptor extends String2FieldBridgeAdaptor implements TwoWayFieldBridge {

	private final TwoWayStringBridge stringBridge;

	public TwoWayString2FieldBridgeAdaptor(TwoWayStringBridge stringBridge) {
		super( stringBridge );
		this.stringBridge = stringBridge;
	}

	public String objectToString(Object object) {
		return stringBridge.objectToString( object );
	}

	public Object get(String name, Document document) {
		Field field = document.getField( name );
		if (field == null) {
			return stringBridge.stringToObject( null );
		}
		else {
			String stringValue;
			if ( field.isBinary() ) {
				try {
					stringValue = CompressionTools.decompressString( field.getBinaryValue() );
				}
				catch (DataFormatException e) {
					throw new SearchException( "Field " + name + " looks like binary but couldn't be decompressed" );
				}
			}
			else {
				stringValue = field.stringValue();
			}
			return stringBridge.stringToObject( stringValue );
		}
	}

	public TwoWayStringBridge unwrap() {
		return stringBridge;
	}
	
}
