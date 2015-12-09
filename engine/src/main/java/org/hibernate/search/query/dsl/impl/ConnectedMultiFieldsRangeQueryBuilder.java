/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.query.dsl.impl;

import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermRangeQuery;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.bridge.spi.ConversionContext;
import org.hibernate.search.bridge.util.impl.ContextualExceptionBridgeHelper;
import org.hibernate.search.bridge.util.impl.NumericFieldUtils;
import org.hibernate.search.engine.metadata.impl.DocumentFieldMetadata;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.query.dsl.RangeTerminationExcludable;

/**
 * @author Emmanuel Bernard
 * @author Sanne Grinovero
 */
public class ConnectedMultiFieldsRangeQueryBuilder implements RangeTerminationExcludable {
	private final RangeQueryContext rangeContext;
	private final QueryCustomizer queryCustomizer;
	private final List<FieldContext> fieldContexts;
	private final QueryBuildingContext queryContext;

	public ConnectedMultiFieldsRangeQueryBuilder(RangeQueryContext rangeContext,
			QueryCustomizer queryCustomizer,
			List<FieldContext> fieldContexts,
			QueryBuildingContext queryContext) {
		this.rangeContext = rangeContext;
		this.queryCustomizer = queryCustomizer;
		this.fieldContexts = fieldContexts;
		this.queryContext = queryContext;
	}

	@Override
	public RangeTerminationExcludable excludeLimit() {
		if ( rangeContext.getFrom() != null && rangeContext.getTo() != null ) {
			rangeContext.setExcludeTo( true );
		}
		else if ( rangeContext.getFrom() != null ) {
			rangeContext.setExcludeFrom( true );
		}
		else if ( rangeContext.getTo() != null ) {
			rangeContext.setExcludeTo( true );
		}
		else {
			throw new AssertionFailure( "Both from and to clause of a range query are null" );
		}
		return this;
	}

	@Override
	public Query createQuery() {
		final int size = fieldContexts.size();
		final ConversionContext conversionContext = new ContextualExceptionBridgeHelper();
		if ( size == 1 ) {
			return queryCustomizer.setWrappedQuery( createQuery( fieldContexts.get( 0 ), conversionContext ) ).createQuery();
		}
		else {
			BooleanQuery aggregatedFieldsQuery = new BooleanQuery( );
			for ( FieldContext fieldContext : fieldContexts ) {
				aggregatedFieldsQuery.add( createQuery( fieldContext, conversionContext ), BooleanClause.Occur.SHOULD );
			}
			return queryCustomizer.setWrappedQuery( aggregatedFieldsQuery ).createQuery();
		}
	}

	private Query createQuery(FieldContext fieldContext, ConversionContext conversionContext) {
		final Query perFieldQuery;
		final String fieldName = fieldContext.getField();

		final DocumentBuilderIndexedEntity documentBuilder = Helper.getDocumentBuilder( queryContext );
		DocumentFieldMetadata fieldMetadata = documentBuilder.getTypeMetadata().getDocumentFieldMetadataFor( fieldName );
		if ( fieldMetadata != null ) {
			if ( fieldMetadata.isNumeric() ) {
				perFieldQuery = createNumericRangeQuery( fieldName, rangeContext );
			}
			else {
				perFieldQuery = createKeywordRangeQuery( fieldName, rangeContext, queryContext, conversionContext, fieldContext );
			}
		}
		else {
			//we need to guess the proper type from the parameter types (Fallback logic required by the protobuf integration in Infinispan)
			if ( rangeBoundaryTypeRequiredNumericQuery( rangeContext.getFrom(), rangeContext.getTo() ) ) {
				perFieldQuery = createNumericRangeQuery( fieldName, rangeContext );
			}
			else {
				perFieldQuery = createKeywordRangeQuery( fieldName, rangeContext, queryContext, conversionContext, fieldContext );
			}
		}

		return fieldContext.getFieldCustomizer().setWrappedQuery( perFieldQuery ).createQuery();
	}

	private boolean rangeBoundaryTypeRequiredNumericQuery(Object from, Object to) {
		if ( from != null ) {
			return NumericFieldUtils.requiresNumericRangeQuery( from );
		}
		else if ( to != null ) {
			return NumericFieldUtils.requiresNumericRangeQuery( to );
		}
		return false;
	}

	private static Query createKeywordRangeQuery(String fieldName, RangeQueryContext rangeContext, QueryBuildingContext queryContext, ConversionContext conversionContext, FieldContext fieldContext) {
		final Analyzer queryAnalyzer = queryContext.getQueryAnalyzer();
		final DocumentBuilderIndexedEntity documentBuilder = Helper.getDocumentBuilder( queryContext );
		final String fromString = rangeContext.hasFrom() ?
				fieldContext.objectToString( documentBuilder, rangeContext.getFrom(), conversionContext ) :
				null;
		final String lowerTerm = fromString == null ?
				null :
				Helper.getAnalyzedTerm( fieldName, fromString, "from", queryAnalyzer, fieldContext );

		final String toString = rangeContext.hasTo() ?
				fieldContext.objectToString( documentBuilder, rangeContext.getTo(), conversionContext ) :
				null;
		final String upperTerm = toString == null ?
				null :
				Helper.getAnalyzedTerm( fieldName, toString, "to", queryAnalyzer, fieldContext );

		return TermRangeQuery.newStringRange( fieldName, lowerTerm, upperTerm, !rangeContext.isExcludeFrom(), !rangeContext.isExcludeTo() );
	}

	private static Query createNumericRangeQuery(String fieldName, RangeQueryContext rangeContext) {
		return NumericFieldUtils.createNumericRangeQuery(
				fieldName,
				rangeContext.getFrom(),
				rangeContext.getTo(),
				!rangeContext.isExcludeFrom(),
				!rangeContext.isExcludeTo()
		);
	}
}
