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

import org.hibernate.search.util.StringHelper;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.annotations.Resolution;
import org.hibernate.search.bridge.ParameterizedBridge;
import org.hibernate.search.bridge.TwoWayStringBridge;

/**
 * Bridge a {@code java.util.Date} to a {@code String}, truncated to the specified resolution.
 * GMT is used as time zone.
 * <p/>
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
//TODO split into StringBridge and TwoWayStringBridge?
public class DateBridge implements TwoWayStringBridge, ParameterizedBridge {

	public static final TwoWayStringBridge DATE_YEAR = new DateBridge( Resolution.YEAR );
	public static final TwoWayStringBridge DATE_MONTH = new DateBridge( Resolution.MONTH );
	public static final TwoWayStringBridge DATE_DAY = new DateBridge( Resolution.DAY );
	public static final TwoWayStringBridge DATE_HOUR = new DateBridge( Resolution.HOUR );
	public static final TwoWayStringBridge DATE_MINUTE = new DateBridge( Resolution.MINUTE );
	public static final TwoWayStringBridge DATE_SECOND = new DateBridge( Resolution.SECOND );
	public static final TwoWayStringBridge DATE_MILLISECOND = new DateBridge( Resolution.MILLISECOND );

	private DateTools.Resolution resolution;

	public DateBridge() {
	}

	public DateBridge(Resolution resolution) {
		this.resolution = DateResolutionUtil.getLuceneResolution( resolution );
	}

	@Override
	public Object stringToObject(String stringValue) {
		if ( StringHelper.isEmpty( stringValue ) ) {
			return null;
		}
		try {
			return DateTools.stringToDate( stringValue );
		}
		catch (ParseException e) {
			throw new SearchException( "Unable to parse into date: " + stringValue, e );
		}
	}

	@Override
	public String objectToString(Object object) {
		return object != null ?
				DateTools.dateToString( (Date) object, resolution ) :
				null;
	}

	@Override
	public void setParameterValues(Map<String,String> parameters) {
		String resolution = parameters.get( "resolution" );
		Resolution hibResolution = Resolution.valueOf( resolution.toUpperCase( Locale.ENGLISH ) );
		this.resolution = DateResolutionUtil.getLuceneResolution( hibResolution );
	}
}
