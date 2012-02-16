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

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.lucene.document.Document;
import org.hibernate.annotations.common.AssertionFailure;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XMember;
import org.hibernate.search.bridge.BridgeException;
import org.hibernate.search.bridge.ConversionContext;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.StringBridge;
import org.hibernate.search.bridge.TwoWayFieldBridge;

/**
 * Wrap the exception with an exception provide contextual feedback
 *
 * @author Emmanuel Bernard
 * @author Sanne Grinovero
 */
public class ContextualException2WayBridge implements TwoWayFieldBridge, ConversionContext {
	
	private static final NamedVirtualXMember IDENTIFIER = new NamedVirtualXMember( "identifier" );

	private enum OperatingMode { STRING, TWO_WAY, ONE_WAY, NOTSET };

	private Class<?> clazz;
	private List<XMember> path = new ArrayList<XMember>( 5 ); //half of usual increment size as I don't expect much
	private String fieldName;
	private StringBridge stringBridge;
	private FieldBridge oneWayBridge;
	private TwoWayFieldBridge twoWayBridge;
	private OperatingMode mode = OperatingMode.NOTSET;

	public ConversionContext setClass(Class<?> clazz) {
		this.clazz = clazz;
		return this;
	}

	public ConversionContext setFieldName(String fieldName) {
		this.fieldName = fieldName;
		return this;
	}

	public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
		if ( mode == OperatingMode.TWO_WAY ) {
			try {
				twoWayBridge.set( name, value, document, luceneOptions );
			}
			catch (RuntimeException e) {
				throw buildBridgeException( e, "set" );
			}
		}
		else if ( mode == OperatingMode.ONE_WAY ) {
			try {
				oneWayBridge.set( name, value, document, luceneOptions );
			}
			catch (RuntimeException e) {
				throw buildBridgeException( e, "set" );
			}
		}
		else {
			throw failUnexpectedMode();
		}
	}

	public ConversionContext pushMethod(XMember xMember) {
		path.add( xMember );
		return this;
	}

	public ConversionContext popMethod() {
		path.remove( path.size() - 1 );
		return this;
	}

	public Object get(String name, Document document) {
		if ( mode == OperatingMode.TWO_WAY ) {
			try {
				return twoWayBridge.get(name, document);
			}
			catch (RuntimeException e) {
				throw buildBridgeException(e, "get");
			}
		}
		else {
			throw failUnexpectedMode();
		}
	}

	public String objectToString(Object object) {
		try {
			if ( mode == OperatingMode.TWO_WAY ) {
				return twoWayBridge.objectToString(object);
			}
			else if ( mode == OperatingMode.STRING ) {
				return stringBridge.objectToString(object);
			}
		}
		catch (Exception e) {
			throw buildBridgeException(e, "objectToString");
		}
		throw failUnexpectedMode();
	}

	@Override
	public ConversionContext setStringBridge(StringBridge bridge) {
		this.mode = OperatingMode.STRING;
		this.stringBridge = bridge;
		this.twoWayBridge = null;
		this.oneWayBridge = null;
		return this;
	}

	@Override
	public ConversionContext setFieldBridge(FieldBridge fieldBridge) {
		this.mode = OperatingMode.ONE_WAY;
		this.stringBridge = null;
		this.twoWayBridge = null;
		this.oneWayBridge = fieldBridge;
		return this;
	}

	@Override
	public ContextualException2WayBridge setFieldBridge(TwoWayFieldBridge delegate) {
		this.mode = OperatingMode.TWO_WAY;
		this.stringBridge = null;
		this.twoWayBridge = delegate;
		this.oneWayBridge = null;
		return this;
	}

	public ConversionContext pushIdentifierMethod() {
		pushMethod( IDENTIFIER );
		return this;
	}

	private AssertionFailure failUnexpectedMode() {
		return new AssertionFailure( "Unexpected invocation in current state " + mode );
	}

	protected BridgeException buildBridgeException(Exception e, String method) {
		StringBuilder error = new StringBuilder( "Exception while calling bridge#" );
		error.append( method );
		if ( clazz != null ) {
			error.append( "\n\tclass: " ).append( clazz.getName() );
		}
		if ( path.size() > 0 ) {
			error.append( "\n\tpath: " );
			for( XMember pathNode : path ) {
				error.append( pathNode.getName() ).append( "." );
			}
			error.deleteCharAt( error.length() - 1 );
		}
		if ( fieldName != null ) {
			error.append( "\n\tfield bridge: " ).append( fieldName );
		}
		throw new BridgeException( error.toString(), e );
	}

	private static class NamedVirtualXMember implements XMember {
		
		private final String name;

		NamedVirtualXMember(String name) {
			this.name = name;
		}

		public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
			throw new UnsupportedOperationException();
		}

		public <T extends Annotation> boolean isAnnotationPresent(Class<T> annotationType) {
			throw new UnsupportedOperationException();
		}

		public Annotation[] getAnnotations() {
			throw new UnsupportedOperationException();
		}

		public String getName() {
			return name;
		}

		public boolean isCollection() {
			return false;
		}

		public boolean isArray() {
			return false;
		}

		public Class<? extends Collection> getCollectionClass() {
			throw new UnsupportedOperationException();
		}

		public XClass getType() {
			throw new UnsupportedOperationException();
		}

		public XClass getElementClass() {
			throw new UnsupportedOperationException();
		}

		public XClass getClassOrElementClass() {
			throw new UnsupportedOperationException();
		}

		public XClass getMapKey() {
			throw new UnsupportedOperationException();
		}

		public int getModifiers() {
			throw new UnsupportedOperationException();
		}

		public void setAccessible(boolean accessible) {
		}

		public Object invoke(Object target, Object... parameters) {
			throw new UnsupportedOperationException();
		}

		public boolean isTypeResolved() {
			throw new UnsupportedOperationException();
		}
		
	}

}
