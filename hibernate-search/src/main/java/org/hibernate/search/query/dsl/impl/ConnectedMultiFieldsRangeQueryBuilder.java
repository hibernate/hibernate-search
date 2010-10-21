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

import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermRangeQuery;

import org.hibernate.annotations.common.AssertionFailure;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.NumericFieldBridge;
import org.hibernate.search.engine.DocumentBuilderIndexedEntity;
import org.hibernate.search.query.dsl.RangeTerminationExcludable;

/**
 * @author Emmanuel Bernard
 */
public class ConnectedMultiFieldsRangeQueryBuilder implements RangeTerminationExcludable {
	private final RangeQueryContext rangeContext;
	private final QueryCustomizer queryCustomizer;
	private final List<FieldContext> fieldContexts;
	private final QueryBuildingContext queryContext;

	public ConnectedMultiFieldsRangeQueryBuilder(RangeQueryContext rangeContext,
												 QueryCustomizer queryCustomizer, List<FieldContext> fieldContexts,
												 QueryBuildingContext queryContext) {
		this.rangeContext = rangeContext;
		this.queryCustomizer = queryCustomizer;
		this.fieldContexts = fieldContexts;
		this.queryContext = queryContext;
	}

	public RangeTerminationExcludable excludeLimit() {
		if ( rangeContext.getFrom() != null && rangeContext.getTo() != null ) {
			rangeContext.setExcludeTo( true );
		}
		else if ( rangeContext.getFrom() != null ) {
			rangeContext.setExcludeTo( true );
		}
		else if ( rangeContext.getTo() != null ) {
			rangeContext.setExcludeTo( true );
		}
		else {
			throw new AssertionFailure( "Both from and to clause of a range query are null" );
		}
		return this;
	}

	public Query createQuery() {
		final int size = fieldContexts.size();
		if ( size == 1 ) {
			return queryCustomizer.setWrappedQuery( createQuery( fieldContexts.get( 0 ) ) ).createQuery();
		}
		else {
			BooleanQuery aggregatedFieldsQuery = new BooleanQuery( );
			for ( FieldContext fieldContext : fieldContexts ) {
				aggregatedFieldsQuery.add( createQuery( fieldContext ), BooleanClause.Occur.SHOULD );
			}
			return  queryCustomizer.setWrappedQuery( aggregatedFieldsQuery ).createQuery();
		}
	}

	public Query createQuery(FieldContext fieldContext) {
		final Query perFieldQuery;
		final String fieldName = fieldContext.getField();
		final Analyzer queryAnalyzer = queryContext.getQueryAnalyzer();

		final DocumentBuilderIndexedEntity<?> documentBuilder = Helper.getDocumentBuilder( queryContext );

		final Object fromObject = rangeContext.getFrom();
		final String fromString  = fieldContext.isIgnoreFieldBridge() ?
				fromObject == null ? null : fromObject.toString() :
				documentBuilder.objectToString( fieldName, fromObject );
		final String lowerTerm = fromString == null ?
				null :
				Helper.getAnalyzedTerm( fieldName, fromString, "from", queryAnalyzer, fieldContext );

		final Object toObject = rangeContext.getTo();
		final String toString  = fieldContext.isIgnoreFieldBridge() ?
				toObject == null ? null : toObject.toString() :
				documentBuilder.objectToString( fieldName, toObject );
		final String upperTerm = toString == null ?
				null :
				Helper.getAnalyzedTerm( fieldName, toString, "to", queryAnalyzer, fieldContext );

		FieldBridge fieldBridge = documentBuilder.getBridge(fieldName);
		if(NumericFieldBridge.class.isAssignableFrom(FieldBridge.class)) {
			    perFieldQuery = rangeForNumeric(fieldName, fromObject, toObject, fieldBridge, !rangeContext.isExcludeFrom(),!rangeContext.isExcludeTo() );
		} else {

				perFieldQuery = new TermRangeQuery(
					fieldName,
					lowerTerm,
					upperTerm,
					!rangeContext.isExcludeFrom(),
					!rangeContext.isExcludeTo()
				);
		}
		return fieldContext.getFieldCustomizer().setWrappedQuery( perFieldQuery ).createQuery();
	}


	public NumericRangeQuery rangeForNumeric(String fieldName, Object from, Object to,
											 FieldBridge fieldBridge, boolean includeLower, boolean includeUpper) {
		Class clazz = ( (NumericFieldBridge) fieldBridge ).getClazz();
		if(clazz.isAssignableFrom(Double.class)) {
			return NumericRangeQuery.newDoubleRange(fieldName,(Double)from,(Double)to,includeLower,includeUpper);
		 }
		if(clazz.isAssignableFrom(Long.class)) {
			return NumericRangeQuery.newLongRange(fieldName,(Long)from,(Long)to,includeLower,includeUpper);
		 }
		if(clazz.isAssignableFrom(Integer.class)) {
			return NumericRangeQuery.newIntRange(fieldName,(Integer)from,(Integer)to,includeLower,includeUpper);
		 }
		if(clazz.isAssignableFrom(Float.class)) {
			return NumericRangeQuery.newFloatRange(fieldName,(Float)from,(Float)to,includeLower,includeUpper) ;
		}
		return null;
	}


}
