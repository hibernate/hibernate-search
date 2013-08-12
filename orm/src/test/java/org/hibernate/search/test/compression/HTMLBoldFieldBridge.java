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
package org.hibernate.search.test.compression;

import java.util.zip.DataFormatException;

import org.apache.lucene.document.CompressionTools;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Fieldable;

import org.hibernate.search.SearchException;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.TwoWayFieldBridge;

/**
 * This FieldBridge is storing strings in the index wrapping the entity value in html bold tags.
 *
 * @author Sanne Grinovero
 * @see LuceneOptions
 */
public class HTMLBoldFieldBridge implements FieldBridge, TwoWayFieldBridge {

	@Override
	public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
		String fieldValue = objectToString( value );
		luceneOptions.addFieldToDocument( name, fieldValue, document );
	}

	@Override
	public Object get(String name, Document document) {
		Fieldable field = document.getFieldable( name );
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
			return stringValue.substring( 3, stringValue.length() - 4 );
	}

	@Override
	public String objectToString(Object value) {
		String originalValue = value.toString();
		return "<b>" + originalValue + "</b>";
	}
}
