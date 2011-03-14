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
import org.hibernate.annotations.common.reflection.XMember;
import org.hibernate.search.bridge.BridgeException;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.LuceneOptions;

import java.util.LinkedList;
import java.util.List;

/**
 * Wrap the exception with an exception provide contextual feedback
 *
 * @author Emmanuel Bernard
 */
public class ContextualExceptionBridge implements FieldBridge {
	private FieldBridge delegate;
	protected Class<?> clazz;
	protected List<XMember> path = new LinkedList<XMember>();
	protected String fieldName;

	public ContextualExceptionBridge setFieldBridge(FieldBridge delegate) {
		this.delegate = delegate;
		return this;
	}

	public ContextualExceptionBridge setClass(Class<?> clazz) {
		this.clazz = clazz;
		return this;
	}

	public ContextualExceptionBridge setFieldName(String fieldName) {
		this.fieldName = fieldName;
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

	public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
		try {
			delegate.set( name, value, document, luceneOptions );
		}
		catch (Exception e) {
			throw buildBridgeException( e, "set" );
		}
	}

	public ContextualExceptionBridge pushMethod(XMember xMember) {
		path.add( xMember );
		return this;
	}

	public ContextualExceptionBridge popMethod() {
		path.remove( path.size() - 1 );
		return this;
	}

}
