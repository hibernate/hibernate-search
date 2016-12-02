/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.bridge.builtin.impl;

import java.util.Calendar;
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
 * Bridge a {@code java.util.Calendar} to a {@code String} using the ISO 8601 standard which is the default date format
 * of Elasticsearch. The time zone of the calendar is stored in the index.
 *
 * Typically, a {@code java.util.Calendar} will be converted to "2016-04-28T15:24:25+01:00".
 *
 * @author Guillaume Smet
 */
public class ElasticsearchCalendarBridge
		extends EncodingStringBridge<Calendar>
		implements TwoWayFieldBridge, ParameterizedBridge, IgnoreAnalyzerBridge {

	private static final Log LOG = LoggerFactory.make( Log.class );

	public static final ElasticsearchCalendarBridge DATE_YEAR = new ElasticsearchCalendarBridge( Resolution.YEAR );
	public static final ElasticsearchCalendarBridge DATE_MONTH = new ElasticsearchCalendarBridge( Resolution.MONTH );
	public static final ElasticsearchCalendarBridge DATE_DAY = new ElasticsearchCalendarBridge( Resolution.DAY );
	public static final ElasticsearchCalendarBridge DATE_HOUR = new ElasticsearchCalendarBridge( Resolution.HOUR );
	public static final ElasticsearchCalendarBridge DATE_MINUTE = new ElasticsearchCalendarBridge( Resolution.MINUTE );
	public static final ElasticsearchCalendarBridge DATE_SECOND = new ElasticsearchCalendarBridge( Resolution.SECOND );
	public static final ElasticsearchCalendarBridge DATE_MILLISECOND = new ElasticsearchCalendarBridge( Resolution.MILLISECOND );

	private DateTools.Resolution resolution;

	public ElasticsearchCalendarBridge() {
		this( Resolution.MILLISECOND );
	}

	public ElasticsearchCalendarBridge(Resolution resolution) {
		this.resolution = DateResolutionUtil.getLuceneResolution( resolution );
	}

	@Override
	public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
		if ( value == null ) {
			return;
		}

		luceneOptions.addFieldToDocument( name, convertToString( (Calendar) value ), document );
	}

	@Override
	public Object get(String name, Document document) {
		return convertFromString( document.get( name ) );
	}

	@Override
	public String objectToString(Object object) {
		return convertToString( (Calendar) object );
	}

	private String convertToString(Calendar value) {
		return ElasticsearchDateHelper.calendarToString( ElasticsearchDateHelper.round( value, resolution ) );
	}

	private Calendar convertFromString(String value) {
		return ElasticsearchDateHelper.stringToCalendar( value );
	}

	@Override
	public void setParameterValues(Map<String, String> parameters) {
		String resolution = parameters.get( "resolution" );
		Resolution hibResolution = Resolution.valueOf( resolution.toUpperCase( Locale.ENGLISH ) );
		this.resolution = DateResolutionUtil.getLuceneResolution( hibResolution );
	}

	@Override
	protected Calendar parseIndexNullAs(String indexNullAs) throws IllegalArgumentException {
		try {
			return convertFromString( indexNullAs );
		}
		catch (IllegalArgumentException e) {
			throw LOG.invalidNullMarkerForCalendarAndDate( e );
		}
	}

}
