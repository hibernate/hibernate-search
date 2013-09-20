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

package org.hibernate.search.query.dsl.impl;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.query.dsl.RangeMatchingContext;
import org.hibernate.search.query.dsl.RangeTerminationExcludable;

/**
 * @author Emmanuel Bernard
 */
public class ConnectedRangeMatchingContext implements RangeMatchingContext, FieldBridgeCustomization<RangeMatchingContext> {
	private final QueryBuildingContext queryContext;
	private final QueryCustomizer queryCustomizer;
	private final RangeQueryContext rangeContext;
	private final List<FieldContext> fieldContexts;
	//when a varargs of fields are passed, apply the same customization for all.
	//keep the index of the first context in this queue
	private int firstOfContext = 0;

	public ConnectedRangeMatchingContext(String fieldName,
										QueryCustomizer queryCustomizer,
										QueryBuildingContext queryContext) {
		this.queryContext = queryContext;
		this.queryCustomizer = queryCustomizer;
		this.rangeContext = new RangeQueryContext();
		this.fieldContexts = new ArrayList<FieldContext>(4);
		this.fieldContexts.add( new FieldContext( fieldName ) );
	}

	@Override
	public RangeMatchingContext andField(String field) {
		this.fieldContexts.add( new FieldContext( field ) );
		this.firstOfContext = fieldContexts.size() - 1;
		return this;
	}

	@Override
	public <T> FromRangeContext<T> from(T from) {
		rangeContext.setFrom( from );
		return new ConnectedFromRangeContext<T>(this);
	}

	static class ConnectedFromRangeContext<T> implements FromRangeContext<T> {
		private final ConnectedRangeMatchingContext mother;

		ConnectedFromRangeContext(ConnectedRangeMatchingContext mother) {
			this.mother = mother;
		}

		@Override
		public RangeTerminationExcludable to(T to) {
			mother.rangeContext.setTo( to );
			return new ConnectedMultiFieldsRangeQueryBuilder(
					mother.rangeContext,
					mother.queryCustomizer,
					mother.fieldContexts,
					mother.queryContext);
		}

		@Override
		public FromRangeContext<T> excludeLimit() {
			mother.rangeContext.setExcludeFrom( true );
			return this;
		}
	}

	@Override
	public RangeTerminationExcludable below(Object below) {
		rangeContext.setTo( below );
		return new ConnectedMultiFieldsRangeQueryBuilder( rangeContext, queryCustomizer, fieldContexts, queryContext);
	}

	@Override
	public RangeTerminationExcludable above(Object above) {
		rangeContext.setFrom( above );
		return new ConnectedMultiFieldsRangeQueryBuilder( rangeContext, queryCustomizer, fieldContexts, queryContext);
	}

	@Override
	public RangeMatchingContext boostedTo(float boost) {
		for ( FieldContext fieldContext : getCurrentFieldContexts() ) {
			fieldContext.getFieldCustomizer().boostedTo( boost );
		}
		return this;
	}

	private List<FieldContext> getCurrentFieldContexts() {
		return fieldContexts.subList( firstOfContext, fieldContexts.size() );
	}

	@Override
	public RangeMatchingContext ignoreAnalyzer() {
		for ( FieldContext fieldContext : getCurrentFieldContexts() ) {
			fieldContext.setIgnoreAnalyzer( true );
		}
		return this;
	}

	@Override
	public RangeMatchingContext ignoreFieldBridge() {
		for ( FieldContext fieldContext : getCurrentFieldContexts() ) {
			fieldContext.setIgnoreFieldBridge( true );
		}
		return this;
	}

	@Override
	public RangeMatchingContext withFieldBridge(FieldBridge fieldBridge) {
		for ( FieldContext fieldContext : getCurrentFieldContexts() ) {
			fieldContext.setFieldBridge( fieldBridge );
		}
		return this;
	}
}
