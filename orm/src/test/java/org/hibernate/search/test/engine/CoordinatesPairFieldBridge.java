/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
package org.hibernate.search.test.engine;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Fieldable;

import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.TwoWayFieldBridge;

/**
 * Assumes values are strings containing integer pairs in the form "12;34"
 * (strongly assumes valid format)
 */
public class CoordinatesPairFieldBridge implements TwoWayFieldBridge {

	@Override
	public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
		String[] coordinates = value.toString().split( ";" );
		Double x = Double.parseDouble( coordinates[0] );
		Double y = Double.parseDouble( coordinates[1] );
		luceneOptions.addNumericFieldToDocument( getXFieldName( name ), x, document );
		luceneOptions.addNumericFieldToDocument( getYFieldName( name ), y, document );
	}

	@Override
	public Object get(String name, Document document) {
		StringBuilder sb = new StringBuilder( 7 );
		Fieldable xFieldable = document.getFieldable( getXFieldName( name ) );
		Fieldable yFieldable = document.getFieldable( getYFieldName( name ) );
		appendValue( xFieldable, sb );
		sb.append( ';' );
		appendValue( yFieldable, sb );
		return sb.toString();
	}

	private void appendValue(final Fieldable field, final StringBuilder sb) {
		if ( field != null ) {
			sb.append( field.stringValue() );
		}
		else {
			sb.append( '0' );
		}
	}

	@Override
	public String objectToString(Object object) {
		return object.toString();
	}

	private String getYFieldName(final String name) {
		return name + "_y";
	}

	private String getXFieldName(final String name) {
		return name + "_x";
	}

}
