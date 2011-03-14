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

package org.hibernate.search.bridge.util;

import java.lang.annotation.Annotation;
import java.util.Collection;

import org.apache.lucene.document.Document;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XMember;
import org.hibernate.search.bridge.StringBridge;
import org.hibernate.search.bridge.TwoWayFieldBridge;

/**
 * Wrap the exception with an exception provide contextual feedback
 *
 * @author Emmanuel Bernard
 */
public class ContextualException2WayBridge extends ContextualExceptionBridge implements TwoWayFieldBridge {
	
	private static final NamedVirtualXMember IDENTIFIER = new NamedVirtualXMember( "identifier" );
	
	private TwoWayFieldBridge delegate;
	private StringBridge stringBridge;

	public ContextualException2WayBridge setFieldBridge(TwoWayFieldBridge delegate) {
		super.setFieldBridge(delegate);
		this.delegate = delegate;
		this.stringBridge = null;
		return this;
	}

	public ContextualException2WayBridge setClass(Class<?> clazz) {
		super.setClass(clazz);
		return this;
	}

	public ContextualException2WayBridge setFieldName(String fieldName) {
		super.setFieldName(fieldName);
		return this;
	}

	public Object get(String name, Document document) {
		try {
			return delegate.get(name, document);
		}
		catch (Exception e) {
			throw buildBridgeException(e, "get");
		}
	}

	public String objectToString(Object object) {
		try {
			if (delegate != null) {
				return delegate.objectToString(object);
			}
			else {
				return stringBridge.objectToString(object);
			}
		}
		catch (Exception e) {
			throw buildBridgeException(e, "objectToString");
		}
	}

	public ContextualException2WayBridge pushMethod(XMember xMember) {
		super.pushMethod( xMember );
		return this;
	}

	public ContextualException2WayBridge popMethod() {
		super.popMethod();
		return this;
	}

	//FIXME yuk, create a cleaner inheritance for a ContextualExceptionStringBridge
	public ContextualException2WayBridge setStringBridge(StringBridge bridge) {
		this.stringBridge = bridge;
		this.delegate = null;
		return this;
	}

	public ContextualException2WayBridge pushIdentifierMethod() {
		super.pushMethod( IDENTIFIER );
		return this;
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
