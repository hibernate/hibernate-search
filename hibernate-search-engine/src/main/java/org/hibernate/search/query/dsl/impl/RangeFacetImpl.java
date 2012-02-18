/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat, Inc. and/or its affiliates or third-party contributors as
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

import java.util.Date;

import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermRangeQuery;

import org.hibernate.annotations.common.AssertionFailure;
import org.hibernate.search.bridge.spi.ConversionContext;
import org.hibernate.search.bridge.util.impl.ContextualExceptionBridgeHelper;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.query.facet.RangeFacet;

/**
 * @author Hardy Ferentschik
 */
public class RangeFacetImpl<T> extends AbstractFacet implements RangeFacet<T> {
	/**
	 * The facet range, speak the min and max values for this range facet
	 */
	private final FacetRange<T> range;

	/**
	 * The index of the specified ranges
	 */
	private final int rangeIndex;

	/**
	 * The document builder.
	 */
	private final DocumentBuilderIndexedEntity<?> documentBuilder;

	RangeFacetImpl(String facetingName, String fieldName, FacetRange<T> range, int count, int index, DocumentBuilderIndexedEntity<?> documentBuilder) {
		super( facetingName, fieldName, range.getRangeString(), count );
		this.range = range;
		this.rangeIndex = index;
		this.documentBuilder = documentBuilder;
	}

	@Override
	public Query getFacetQuery() {
		Object minOrMax = getNonNullMinOrMax( range );
		if ( minOrMax instanceof Number ) {
			return createNumericRangeQuery();
		}
		else if ( minOrMax instanceof String ) {
			return createRangeQuery(
					(String) range.getMin(),
					(String) range.getMax(),
					range.isMinIncluded(),
					range.isMaxIncluded()
			);
		}
		else if ( minOrMax instanceof Date ) {
			final ConversionContext conversionContext = new ContextualExceptionBridgeHelper();
			return createRangeQuery(
					documentBuilder.objectToString( getFieldName(), range.getMin(), conversionContext ),
					documentBuilder.objectToString( getFieldName(), range.getMax(), conversionContext ),
					range.isMinIncluded(),
					range.isMaxIncluded()
			);
		}
		else {
			throw new AssertionFailure( "Unsupported range type" );
		}
	}

	public int getRangeIndex() {
		return rangeIndex;
	}

	public T getMin() {
		return range.getMin();
	}

	public T getMax() {
		return range.getMax();
	}

	public boolean isIncludeMin() {
		return range.isMinIncluded();
	}

	public boolean isIncludeMax() {
		return range.isMaxIncluded();
	}

	private Object getNonNullMinOrMax(FacetRange<T> range) {
		Object o = range.getMin();
		if ( o == null ) {
			o = range.getMax();
		}
		return o;
	}

	private Query createNumericRangeQuery() {
		NumericRangeQuery query;
		// either end of the range must have a valid value (see also HSEARCH-770)
		Object minOrMax = getNonNullMinOrMax( range );
		if ( minOrMax instanceof Double ) {
			query = NumericRangeQuery.newDoubleRange(
					getFieldName(),
					(Double) range.getMin(),
					(Double) range.getMax(),
					range.isMinIncluded(),
					range.isMaxIncluded()
			);
		}
		else if ( minOrMax instanceof Float ) {
			query = NumericRangeQuery.newFloatRange(
					getFieldName(),
					(Float) range.getMin(),
					(Float) range.getMax(),
					range.isMinIncluded(),
					range.isMaxIncluded()
			);
		}
		else if ( minOrMax instanceof Integer ) {
			query = NumericRangeQuery.newIntRange(
					getFieldName(),
					(Integer) range.getMin(),
					(Integer) range.getMax(),
					range.isMinIncluded(),
					range.isMaxIncluded()
			);
		}

		else if ( minOrMax instanceof Long ) {
			query = NumericRangeQuery.newLongRange(
					getFieldName(),
					(Long) range.getMin(),
					(Long) range.getMax(),
					range.isMinIncluded(),
					range.isMaxIncluded()
			);
		}
		else {
			throw new AssertionFailure( "Unsupported range type" );
		}
		return query;
	}

	private Query createRangeQuery(String min, String max, boolean includeMin, boolean includeMax) {
		return new TermRangeQuery(
				getFieldName(),
				min,
				max,
				includeMin,
				includeMax
		);
	}
}


