/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.bridge.builtin;

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

/**
 * Bridge a {@code java.util.Date} truncated to the specified resolution to a numerically indexed {@code long}.
 *
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
 * @author Hardy Ferentschik
 */
public class NumericEncodingDateBridge implements TwoWayFieldBridge, ParameterizedBridge {

	public static final TwoWayFieldBridge DATE_YEAR = new NumericEncodingDateBridge( Resolution.YEAR );
	public static final TwoWayFieldBridge DATE_MONTH = new NumericEncodingDateBridge( Resolution.MONTH );
	public static final TwoWayFieldBridge DATE_DAY = new NumericEncodingDateBridge( Resolution.DAY );
	public static final TwoWayFieldBridge DATE_HOUR = new NumericEncodingDateBridge( Resolution.HOUR );
	public static final TwoWayFieldBridge DATE_MINUTE = new NumericEncodingDateBridge( Resolution.MINUTE );
	public static final TwoWayFieldBridge DATE_SECOND = new NumericEncodingDateBridge( Resolution.SECOND );
	public static final TwoWayFieldBridge DATE_MILLISECOND = new NumericEncodingDateBridge( Resolution.MILLISECOND );

	private DateTools.Resolution resolution;

	public NumericEncodingDateBridge() {
		this( Resolution.MILLISECOND );
	}

	public NumericEncodingDateBridge(Resolution resolution) {
		this.resolution = DateResolutionUtil.getLuceneResolution( resolution );
	}

	@Override
	public Object get(String name, Document document) {
		final IndexableField field = document.getField( name );
		if ( field != null ) {
			return new Date( (long) field.numericValue() );
		}
		else {
			return null;
		}
	}

	@Override
	public String objectToString(Object object) {
		return object != null ? Long.toString( ( (Date) object ).getTime() ) : null;
	}

	@Override
	public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
		if ( value == null ) {
			return;
		}

		Date date = (Date) value;
		long numericDate = DateTools.round( date.getTime(), resolution );
		luceneOptions.addNumericFieldToDocument( name, numericDate, document );
	}

	@Override
	public void setParameterValues(Map<String, String> parameters) {
		String resolution = parameters.get( "resolution" );
		Resolution hibResolution = Resolution.valueOf( resolution.toUpperCase( Locale.ENGLISH ) );
		this.resolution = DateResolutionUtil.getLuceneResolution( hibResolution );
	}

	public DateTools.Resolution getResolution() {
		return resolution;
	}
}
