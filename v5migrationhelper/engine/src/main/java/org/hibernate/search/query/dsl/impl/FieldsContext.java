/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.query.dsl.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.hibernate.search.bridge.FieldBridge;

/**
 * Encapsulate the common field context state capture.
 *
 * @author Emmanuel Bernard
 */
public class FieldsContext implements Iterable<FieldContext> {

	public static final String[] NO_FIELD = new String[0];

	private final QueryBuildingContext queryContext;
	private final List<FieldContext> fieldContexts;
	//when a varargs of fields are passed, apply the same customization for all.
	//keep the index of the first context in this queue
	private int firstOfContext;

	public FieldsContext(String[] fieldNames, QueryBuildingContext queryContext) {
		this.queryContext = queryContext;
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
		fieldContexts.add( new FieldContext( fieldName, queryContext ) );
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
