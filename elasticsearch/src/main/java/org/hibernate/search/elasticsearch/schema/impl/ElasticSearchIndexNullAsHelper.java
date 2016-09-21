/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.schema.impl;

import java.util.Date;

import org.hibernate.search.elasticsearch.logging.impl.Log;
import org.hibernate.search.elasticsearch.schema.impl.model.DataType;
import org.hibernate.search.elasticsearch.util.impl.ElasticsearchDateHelper;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * @author Sanne Grinovero
 * @author Yoann Rodiere
 */
class ElasticSearchIndexNullAsHelper {

	private static final Log LOG = LoggerFactory.make( Log.class );

	private ElasticSearchIndexNullAsHelper() {
		// not to be instantiated
	}

	public static Object getNullValue(String fieldName, DataType dataType, String indexNullAs) {
		switch ( dataType ) {
			case STRING:
				return indexNullAs;
			case DOUBLE:
				return toDouble( indexNullAs, fieldName );
			case FLOAT:
				return toFloat( indexNullAs, fieldName );
			case INTEGER:
				return toInteger( indexNullAs, fieldName );
			case LONG:
				return toLong( indexNullAs, fieldName );
			case DATE:
				return toDate( indexNullAs, fieldName );
			case BOOLEAN:
				return toBoolean( indexNullAs, fieldName );
			default:
				throw new AssertionFailure( "Unexpected Elasticsearch datatype: " + dataType );
		}
	}

	private static Long toLong(String proposedTokenValue, String fieldName) {
		try {
			return Long.parseLong( proposedTokenValue );
		}
		catch (NumberFormatException nfe) {
			throw LOG.nullMarkerNeedsToRepresentALongNumber( proposedTokenValue, fieldName );
		}
	}

	private static Integer toInteger(String proposedTokenValue, String fieldName) {
		try {
			return Integer.parseInt( proposedTokenValue );
		}
		catch (NumberFormatException nfe) {
			throw LOG.nullMarkerNeedsToRepresentAnIntegerNumber( proposedTokenValue, fieldName );
		}
	}

	private static Float toFloat(String proposedTokenValue, String fieldName) {
		try {
			return Float.parseFloat( proposedTokenValue );
		}
		catch (NumberFormatException nfe) {
			throw LOG.nullMarkerNeedsToRepresentAFloatNumber( proposedTokenValue, fieldName );
		}
	}

	private static Double toDouble(String proposedTokenValue, String fieldName) {
		try {
			return Double.parseDouble( proposedTokenValue );
		}
		catch (NumberFormatException nfe) {
			throw LOG.nullMarkerNeedsToRepresentADoubleNumber( proposedTokenValue, fieldName );
		}
	}

	private static Boolean toBoolean(String indexNullAs, String fieldName) {
		if ( Boolean.TRUE.toString().equals( indexNullAs ) ) {
			return Boolean.TRUE;
		}
		else if ( Boolean.FALSE.toString().equals( indexNullAs ) ) {
			return Boolean.FALSE;
		}
		else {
			throw LOG.nullMarkerNeedsToRepresentABoolean( indexNullAs, fieldName );
		}
	}

	private static Date toDate(String indexNullAs, String fieldName) {
		try {
			return ElasticsearchDateHelper.stringToDate( indexNullAs );
		}
		catch (IllegalArgumentException e) {
			throw LOG.nullMarkerNeedsToRepresentADate( indexNullAs, fieldName );
		}
	}

}
