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
package org.hibernate.search.bridge.builtin;

import java.text.ParseException;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import org.apache.lucene.document.DateTools;

import org.hibernate.search.util.StringHelper;
import org.hibernate.search.SearchException;
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
