/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.bridge.builtin;

import java.text.ParseException;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;

import org.hibernate.search.annotations.Resolution;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.ParameterizedBridge;
import org.hibernate.search.bridge.TwoWayFieldBridge;
import org.hibernate.search.bridge.builtin.impl.DateResolutionUtil;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Bridge a {@code java.util.Date} to a {@code String}, truncated to the specified resolution.
 * GMT is used as time zone.
 * <ul>
 * <li>Resolution.YEAR: yyyy</li>
 * <li>Resolution.MONTH: yyyyMM</li>
 * <li>Resolution.DAY: yyyyMMdd</li>
 * <li>Resolution.HOUR: yyyyMMddHH</li>
 * <li>Resolution.MINUTE: yyyyMMddHHmm</li>
 * <li>Resolution.SECOND: yyyyMMddHHmmss</li>
 * <li>Resolution.MILLISECOND: yyyyMMddHHmmssSSS</li>
 * </ul>
 *
 * @author Emmanuel Bernard
 */
public class StringEncodingDateBridge implements TwoWayFieldBridge, ParameterizedBridge {
	private static final Log log = LoggerFactory.make();

	public static final TwoWayFieldBridge DATE_YEAR = new StringEncodingDateBridge( Resolution.YEAR );
	public static final TwoWayFieldBridge DATE_MONTH = new StringEncodingDateBridge( Resolution.MONTH );
	public static final TwoWayFieldBridge DATE_DAY = new StringEncodingDateBridge( Resolution.DAY );
	public static final TwoWayFieldBridge DATE_HOUR = new StringEncodingDateBridge( Resolution.HOUR );
	public static final TwoWayFieldBridge DATE_MINUTE = new StringEncodingDateBridge( Resolution.MINUTE );
	public static final TwoWayFieldBridge DATE_SECOND = new StringEncodingDateBridge( Resolution.SECOND );
	public static final TwoWayFieldBridge DATE_MILLISECOND = new StringEncodingDateBridge( Resolution.MILLISECOND );

	private DateTools.Resolution resolution;

	public StringEncodingDateBridge() {
	}

	public StringEncodingDateBridge(Resolution resolution) {
		this.resolution = DateResolutionUtil.getLuceneResolution( resolution );
	}

	@Override
	public Object get(String name, Document document) {
		final IndexableField field = document.getField( name );
		if ( field != null ) {
			try {
				return DateTools.stringToDate( field.stringValue() );
			}
			catch (ParseException e) {
				throw log.invalidStringDateFieldInDocument( name, field.stringValue() );
			}
		}
		else {
			return null;
		}
	}

	@Override
	public String objectToString(Object object) {
		return object != null ?
				DateTools.dateToString( (Date) object, resolution ) :
				null;
	}

	@Override
	public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
		if ( value == null ) {
			return;
		}

		Date date = (Date) value;
		String stringDate = DateTools.dateToString( date, resolution );
		luceneOptions.addFieldToDocument( name, stringDate, document );
	}

	@Override
	public void setParameterValues(Map<String, String> parameters) {
		String resolution = parameters.get( "resolution" );
		Resolution hibResolution = Resolution.valueOf( resolution.toUpperCase( Locale.ENGLISH ) );
		this.resolution = DateResolutionUtil.getLuceneResolution( hibResolution );
	}
}
