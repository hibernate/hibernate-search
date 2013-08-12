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

package org.hibernate.search.bridge.builtin.impl;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Fieldable;

import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.TwoWayFieldBridge;

/**
 * @author Hardy Ferentschik
 */
public class NullEncodingTwoWayFieldBridge implements TwoWayFieldBridge {

	private final TwoWayFieldBridge fieldBridge;
	private final String nullMarker;

	public NullEncodingTwoWayFieldBridge(TwoWayFieldBridge fieldBridge, String nullMarker) {
		this.fieldBridge = fieldBridge;
		this.nullMarker = nullMarker;
	}

	@Override
	public Object get(String name, Document document) {
		Fieldable field = document.getFieldable( name );
		String stringValue = field.stringValue();
		if ( nullMarker.equals( stringValue ) ) {
			return null;
		}
		else {
			return fieldBridge.get( name, document );
		}
	}

	@Override
	public String objectToString(Object object) {
		if ( object == null ) {
			return nullMarker;
		}
		else {
			return fieldBridge.objectToString( object );
		}
	}

	public TwoWayFieldBridge unwrap() {
		return fieldBridge;
	}

	@Override
	public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
		fieldBridge.set( name, value, document, luceneOptions );
	}
}
