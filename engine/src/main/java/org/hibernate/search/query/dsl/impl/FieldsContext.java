/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010-2014, Red Hat, Inc. and/or its affiliates or third-party contributors as
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

package org.hibernate.search.query.dsl.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.hibernate.search.bridge.FieldBridge;

/**
 * Encapsulate the common field context state capture.
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class FieldsContext implements Iterable<FieldContext> {

	public static final String[] NO_FIELD = new String[0];

	private final List<FieldContext> fieldContexts;
	//when a varargs of fields are passed, apply the same customization for all.
	//keep the index of the first context in this queue
	private int firstOfContext;

	public FieldsContext(String[] fieldNames) {
		firstOfContext = 0;
		if ( fieldNames == null ) {
			fieldNames = NO_FIELD;
		}
		fieldContexts = new ArrayList<FieldContext>( fieldNames.length < 4 ? 4 : fieldNames.length );
		for ( String fieldName : fieldNames ) {
			doAdd( fieldName );
		}
	}

	public void add(String fieldName) {
		doAdd( fieldName );
		firstOfContext = fieldContexts.size() - 1;
	}

	private void doAdd(String fieldName) {
		fieldContexts.add( new FieldContext( fieldName ) );
	}

	public void addAll(String... fieldNames) {
		if ( fieldNames.length != 0 ) {
			firstOfContext = fieldContexts.size();
			for ( String fieldName : fieldNames ) {
				doAdd( fieldName );
			}
		}
	}

	public void boostedTo(float boost) {
		for ( FieldContext fieldContext : getCurrentFieldContexts() ) {
			fieldContext.getFieldCustomizer().boostedTo( boost );
		}
	}

	public void ignoreAnalyzer() {
		for ( FieldContext fieldContext : getCurrentFieldContexts() ) {
			fieldContext.setIgnoreAnalyzer( true );
		}
	}

	public void ignoreFieldBridge() {
		for ( FieldContext fieldContext : getCurrentFieldContexts() ) {
			fieldContext.setIgnoreFieldBridge( true );
		}
	}

	public void withFieldBridge(FieldBridge fieldBridge) {
		for ( FieldContext fieldContext : getCurrentFieldContexts() ) {
			fieldContext.setFieldBridge( fieldBridge );
		}
	}

	private List<FieldContext> getCurrentFieldContexts() {
		return fieldContexts.subList( firstOfContext, fieldContexts.size() );
	}

	public FieldContext getFirst() {
		return fieldContexts.get( 0 );
	}

	public int size() {
		return fieldContexts.size();
	}

	@Override
	public Iterator<FieldContext> iterator() {
		return fieldContexts.iterator();
	}
}
