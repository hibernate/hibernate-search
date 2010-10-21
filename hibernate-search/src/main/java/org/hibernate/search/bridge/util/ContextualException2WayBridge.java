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

import org.apache.lucene.document.Document;
import org.hibernate.search.bridge.StringBridge;
import org.hibernate.search.bridge.TwoWayFieldBridge;

/**
 * Wrap the exception with an exception provide contextual feedback
 *
 * @author Emmanuel Bernard
 */
public class ContextualException2WayBridge extends ContextualExceptionBridge implements TwoWayFieldBridge {
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

	public ContextualException2WayBridge pushMethod(String name) {
		super.pushMethod(name);
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
}
