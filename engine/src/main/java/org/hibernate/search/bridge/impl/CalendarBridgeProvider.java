/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.bridge.impl;

import java.lang.reflect.AnnotatedElement;

import org.hibernate.search.annotations.Resolution;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.builtin.CalendarBridge;
import org.hibernate.search.bridge.builtin.impl.TwoWayString2FieldBridgeAdaptor;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Built-in {@link org.hibernate.search.bridge.spi.BridgeProvider} handling calendar bridging when
 * {@code @CalendarBridge} is involved.
 * As built-in provider, no Service Loader file is used: the {@code BridgeFactory} does access it
 * after the custom bridge providers found.
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class CalendarBridgeProvider extends ExtendedBridgeProvider {
	private static final Log LOG = LoggerFactory.make();

	private static final FieldBridge CALENDAR_YEAR = new TwoWayString2FieldBridgeAdaptor( CalendarBridge.CALENDAR_YEAR );
	private static final FieldBridge CALENDAR_MONTH = new TwoWayString2FieldBridgeAdaptor( CalendarBridge.CALENDAR_MONTH );
	private static final FieldBridge CALENDAR_DAY = new TwoWayString2FieldBridgeAdaptor( CalendarBridge.CALENDAR_DAY );
	private static final FieldBridge CALENDAR_HOUR = new TwoWayString2FieldBridgeAdaptor( CalendarBridge.CALENDAR_HOUR );
	private static final FieldBridge CALENDAR_MINUTE = new TwoWayString2FieldBridgeAdaptor( CalendarBridge.CALENDAR_MINUTE );
	private static final FieldBridge CALENDAR_SECOND = new TwoWayString2FieldBridgeAdaptor( CalendarBridge.CALENDAR_SECOND );
	public static final FieldBridge CALENDAR_MILLISECOND = new TwoWayString2FieldBridgeAdaptor( CalendarBridge.CALENDAR_MILLISECOND );

	@Override
	public FieldBridge provideFieldBridge(ExtendedBridgeProviderContext context) {
		AnnotatedElement annotatedElement = context.getAnnotatedElement();
		if ( annotatedElement.isAnnotationPresent( org.hibernate.search.annotations.CalendarBridge.class ) ) {
			Resolution resolution = annotatedElement.getAnnotation( org.hibernate.search.annotations.CalendarBridge.class )
					.resolution();
			return getCalendarField( resolution );
		}
		return null;
	}

	private FieldBridge getCalendarField(Resolution resolution) {
		switch ( resolution ) {
			case YEAR:
				return CALENDAR_YEAR;
			case MONTH:
				return CALENDAR_MONTH;
			case DAY:
				return CALENDAR_DAY;
			case HOUR:
				return CALENDAR_HOUR;
			case MINUTE:
				return CALENDAR_MINUTE;
			case SECOND:
				return CALENDAR_SECOND;
			case MILLISECOND:
				return CALENDAR_MILLISECOND;
			default:
				throw LOG.unknownResolution( resolution.toString() );
		}
	}
}
