/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.impl;

import java.util.Calendar;
import java.util.Date;

import org.apache.lucene.document.Document;
import org.hibernate.search.annotations.Resolution;
import org.hibernate.search.backend.elasticsearch.util.impl.ElasticsearchDateHelper;
import org.hibernate.search.bridge.LuceneOptions;

/**
 * Bridge a {@code java.util.Calendar} to a {@code String} using the ISO 8601 standard which is the default date format
 * of Elasticsearch.
 * UTC is used as time zone.
 * Typically, a {@code java.util.Calendar} will be converted to "2016-04-28T15:24:25Z".
 *
 * @author Guillaume Smet
 */
public class ElasticsearchCalendarBridge extends ElasticsearchDateBridge {

	public static final ElasticsearchCalendarBridge DATE_YEAR = new ElasticsearchCalendarBridge( Resolution.YEAR );
	public static final ElasticsearchCalendarBridge DATE_MONTH = new ElasticsearchCalendarBridge( Resolution.MONTH );
	public static final ElasticsearchCalendarBridge DATE_DAY = new ElasticsearchCalendarBridge( Resolution.DAY );
	public static final ElasticsearchCalendarBridge DATE_HOUR = new ElasticsearchCalendarBridge( Resolution.HOUR );
	public static final ElasticsearchCalendarBridge DATE_MINUTE = new ElasticsearchCalendarBridge( Resolution.MINUTE );
	public static final ElasticsearchCalendarBridge DATE_SECOND = new ElasticsearchCalendarBridge( Resolution.SECOND );
	public static final ElasticsearchCalendarBridge DATE_MILLISECOND = new ElasticsearchCalendarBridge( Resolution.MILLISECOND );

	public ElasticsearchCalendarBridge() {
		super( Resolution.MILLISECOND );
	}

	public ElasticsearchCalendarBridge(Resolution resolution) {
		super( resolution );
	}

	@Override
	public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
		Date date = null;
		if ( value != null ) {
			date = ( (Calendar) value ).getTime();
		}
		super.set( name, date, document, luceneOptions );
	}

	@Override
	public Object get(String name, Document document) {
		Object value = super.get( name, document );
		if ( value != null ) {
			value = ElasticsearchDateHelper.dateToCalendar( (Date) value );
		}
		return value;
	}

	@Override
	public String objectToString(Object object) {
		Date date = null;
		if ( object != null ) {
			date = ( (Calendar) object ).getTime();
		}
		return super.objectToString( date );
	}

}