/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.bridge.builtin.impl;

import java.util.Date;
import java.util.Locale;
import java.util.Map;

import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.Document;
import org.hibernate.search.annotations.Resolution;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.ParameterizedBridge;
import org.hibernate.search.bridge.TwoWayFieldBridge;
import org.hibernate.search.bridge.builtin.impl.DateResolutionUtil;
import org.hibernate.search.bridge.spi.IgnoreAnalyzerBridge;
import org.hibernate.search.bridge.util.impl.EncodingStringBridge;
import org.hibernate.search.elasticsearch.logging.impl.Log;
import org.hibernate.search.elasticsearch.util.impl.ElasticsearchDateHelper;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Bridge a {@code java.util.Date} to a {@code String} using the ISO 8601 standard which is the default date format
 * of Elasticsearch.
 * UTC is used as time zone.
 * Typically, a {@code java.util.Date} will be converted to "2016-04-28T15:24:25Z".
 *
 * @author Guillaume Smet
 */
public class ElasticsearchDateBridge
		extends EncodingStringBridge<Date>
		implements TwoWayFieldBridge, ParameterizedBridge, IgnoreAnalyzerBridge {

	private static final Log LOG = LoggerFactory.make( Log.class );

	public static final ElasticsearchDateBridge DATE_YEAR = new ElasticsearchDateBridge( Resolution.YEAR );
	public static final ElasticsearchDateBridge DATE_MONTH = new ElasticsearchDateBridge( Resolution.MONTH );
	public static final ElasticsearchDateBridge DATE_DAY = new ElasticsearchDateBridge( Resolution.DAY );
	public static final ElasticsearchDateBridge DATE_HOUR = new ElasticsearchDateBridge( Resolution.HOUR );
	public static final ElasticsearchDateBridge DATE_MINUTE = new ElasticsearchDateBridge( Resolution.MINUTE );
	public static final ElasticsearchDateBridge DATE_SECOND = new ElasticsearchDateBridge( Resolution.SECOND );
	public static final ElasticsearchDateBridge DATE_MILLISECOND = new ElasticsearchDateBridge( Resolution.MILLISECOND );

	private DateTools.Resolution resolution;

	public ElasticsearchDateBridge() {
		this( Resolution.MILLISECOND );
	}

	public ElasticsearchDateBridge(Resolution resolution) {
		this.resolution = DateResolutionUtil.getLuceneResolution( resolution );
	}

	@Override
	public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
		if ( value == null ) {
			return;
		}

		luceneOptions.addFieldToDocument( name, convertToString( (Date) value ), document );
	}

	@Override
	public Object get(String name, Document document) {
		return convertFromString( document.get( name ) );
	}

	@Override
	public String objectToString(Object object) {
		return convertToString( (Date) object );
	}

	private String convertToString(Date value) {
		return ElasticsearchDateHelper.dateToString( DateTools.round( value, resolution ) );
	}

	private Date convertFromString(String value) {
		return ElasticsearchDateHelper.stringToDate( value );
	}

	@Override
	public void setParameterValues(Map<String, String> parameters) {
		String resolution = parameters.get( "resolution" );
		Resolution hibResolution = Resolution.valueOf( resolution.toUpperCase( Locale.ENGLISH ) );
		this.resolution = DateResolutionUtil.getLuceneResolution( hibResolution );
	}

	@Override
	protected Date parseIndexNullAs(String indexNullAs) throws IllegalArgumentException {
		try {
			return convertFromString( indexNullAs );
		}
		catch (IllegalArgumentException e) {
			throw LOG.invalidNullMarkerForCalendarAndDate( e );
		}
	}
}
