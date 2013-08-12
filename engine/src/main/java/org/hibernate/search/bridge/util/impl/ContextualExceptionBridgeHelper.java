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

package org.hibernate.search.bridge.util.impl;

import java.util.ArrayList;

import org.apache.lucene.document.Document;

import org.hibernate.search.bridge.BridgeException;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.StringBridge;
import org.hibernate.search.bridge.TwoWayFieldBridge;
import org.hibernate.search.bridge.spi.ConversionContext;

/**
 * Wrap the exception with an exception provide contextual feedback.
 * This class is designed to be reused, but is not threadsafe.
 *
 * @author Emmanuel Bernard
 * @author Sanne Grinovero
 * @author Hardy Ferentschik
 */
public final class ContextualExceptionBridgeHelper implements ConversionContext {

	private static final String IDENTIFIER = "identifier";

	// Mutable state:
	private Class<?> clazz;
	private StringBridge stringBridge;
	private FieldBridge oneWayBridge;
	private TwoWayFieldBridge twoWayBridge;

	//Reused helpers:
	private final ArrayList<String> path = new ArrayList<String>( 5 ); //half of usual increment size as I don't expect much
	private final OneWayConversionContextImpl oneWayAdapter = new OneWayConversionContextImpl();
	private final TwoWayConversionContextImpl twoWayAdapter = new TwoWayConversionContextImpl();
	private final StringConversionContextImpl stringAdapter = new StringConversionContextImpl();

	@Override
	public ConversionContext setClass(Class<?> clazz) {
		this.clazz = clazz;
		return this;
	}

	@Override
	public ConversionContext pushProperty(String property) {
		path.add( property );
		return this;
	}

	@Override
	public ConversionContext popProperty() {
		if ( path.size() == 0 ) {
			throw new IllegalStateException( "Trying to pop a property from an empty conversion context" );
		}
		path.remove( path.size() - 1 );
		return this;
	}

	@Override
	public ConversionContext pushIdentifierProperty() {
		pushProperty( IDENTIFIER );
		return this;
	}

	protected BridgeException buildBridgeException(Exception e, String method) {
		StringBuilder error = new StringBuilder( "Exception while calling bridge#" ).append( method );
		if ( clazz != null ) {
			error.append( "\n\tclass: " ).append( clazz.getName() );
		}
		if ( path.size() > 0 ) {
			error.append( "\n\tpath: " );
			for ( String pathNode : path ) {
				error.append( pathNode ).append( "." );
			}
			error.deleteCharAt( error.length() - 1 );
		}
		throw new BridgeException( error.toString(), e );
	}

	@Override
	public FieldBridge oneWayConversionContext(FieldBridge delegate) {
		this.oneWayBridge = delegate;
		return oneWayAdapter;
	}

	@Override
	public TwoWayFieldBridge twoWayConversionContext(TwoWayFieldBridge delegate) {
		this.twoWayBridge = delegate;
		return twoWayAdapter;
	}

	@Override
	public StringBridge stringConversionContext(StringBridge delegate) {
		this.stringBridge = delegate;
		return stringAdapter;
	}

	private final class OneWayConversionContextImpl implements FieldBridge {

		@Override
		public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
			try {
				oneWayBridge.set( name, value, document, luceneOptions );
			}
			catch (RuntimeException e) {
				throw buildBridgeException( e, "set" );
			}
		}
	}

	private final class TwoWayConversionContextImpl implements TwoWayFieldBridge {

		@Override
		public Object get(String name, Document document) {
			try {
				return twoWayBridge.get( name, document );
			}
			catch (RuntimeException e) {
				throw buildBridgeException( e, "get" );
			}
		}

		@Override
		public String objectToString(Object object) {
			try {
				return twoWayBridge.objectToString( object );
			}
			catch (RuntimeException e) {
				throw buildBridgeException( e, "objectToString" );
			}
		}

		@Override
		public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
			try {
				twoWayBridge.set( name, value, document, luceneOptions );
			}
			catch (RuntimeException e) {
				throw buildBridgeException( e, "set" );
			}
		}
	}

	private final class StringConversionContextImpl implements StringBridge {

		@Override
		public String objectToString(Object object) {
			try {
				return stringBridge.objectToString( object );
			}
			catch (RuntimeException e) {
				throw buildBridgeException( e, "objectToString" );
			}
		}
	}
}
