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

import org.apache.lucene.document.Document;
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
public final class ContextualException2WayBridge implements ConversionContext {

	private static final NamedVirtualXMember IDENTIFIER = new NamedVirtualXMember( "identifier" );

	// Mutable state:
	private Class<?> clazz;
	private String fieldName;
	private StringBridge stringBridge;
	private FieldBridge oneWayBridge;
	private TwoWayFieldBridge twoWayBridge;

	//Reused helpers:
	private final ArrayList<XMember> path = new ArrayList<XMember>( 5 ); //half of usual increment size as I don't expect much
	private final OneWayConversionContextImpl oneWayAdapter = new OneWayConversionContextImpl();
	private final TwoWayConversionContextImpl twoWayAdapter = new TwoWayConversionContextImpl();
	private final StringConversionContextImpl stringAdapter = new StringConversionContextImpl();

	public ConversionContext setClass(Class<?> clazz) {
		this.clazz = clazz;
		return this;
	}

	public ConversionContext setFieldName(String fieldName) {
		this.fieldName = fieldName;
		return this;
	}

	public ConversionContext pushMethod(XMember xMember) {
		path.add( xMember );
		return this;
	}

	public ConversionContext popMethod() {
		path.remove( path.size() - 1 );
		return this;
	}

	public ConversionContext pushIdentifierMethod() {
		pushMethod( IDENTIFIER );
		return this;
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

	@Override
	public OneWayConversionContext oneWayConversionContext(FieldBridge delegate) {
		this.oneWayBridge = delegate;
		return oneWayAdapter;
	}

	@Override
	public TwoWayConversionContext twoWayConversionContext(TwoWayFieldBridge delegate) {
		this.twoWayBridge = delegate;
		return twoWayAdapter;
	}

	@Override
	public StringConversionContext stringConversionContext(StringBridge delegate) {
		this.stringBridge = delegate;
		return stringAdapter;
	}

	private abstract class AbstractConversionContextImpl implements ConversionInvocationContext {

		@Override
		public ConversionInvocationContext setClass(Class<?> beanClass) {
			ContextualException2WayBridge.this.setClass( beanClass );
			return this;
		}

		@Override
		public ConversionInvocationContext setFieldName(String fieldName) {
			ContextualException2WayBridge.this.setFieldName( fieldName );
			return this;
		}

		@Override
		public ConversionInvocationContext pushIdentifierMethod() {
			ContextualException2WayBridge.this.pushIdentifierMethod();
			return this;
		}

		@Override
		public ConversionInvocationContext pushMethod(XMember xmember) {
			ContextualException2WayBridge.this.pushMethod( xmember );
			return this;
		}

		@Override
		public ConversionInvocationContext popMethod() {
			ContextualException2WayBridge.this.popMethod();
			return this;
		}
	}

	private final class OneWayConversionContextImpl extends AbstractConversionContextImpl implements OneWayConversionContext {

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

	private final class TwoWayConversionContextImpl extends AbstractConversionContextImpl implements TwoWayConversionContext {

		@Override
		public Object get(String name, Document document) {
			try {
				return twoWayBridge.get(  name, document );
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

	private final class StringConversionContextImpl extends AbstractConversionContextImpl implements StringConversionContext {

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
