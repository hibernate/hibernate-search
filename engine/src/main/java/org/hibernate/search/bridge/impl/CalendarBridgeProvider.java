/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat, Inc. and/or its affiliates or third-party contributors as
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

package org.hibernate.search.bridge.impl;

import java.lang.reflect.AnnotatedElement;

import org.hibernate.search.annotations.Resolution;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.builtin.CalendarBridge;
import org.hibernate.search.bridge.builtin.impl.TwoWayString2FieldBridgeAdaptor;
import org.hibernate.search.bridge.spi.BridgeProvider;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Built-in {@link BridgeProvider} handling calendar bridging when {@code @CalendarBridge} is involved.
 * As built-in provider, no Service Loader file is used: the {@code BridgeFactory} does access it
 * after the custom bridge providers found.
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class CalendarBridgeProvider implements BridgeProvider {
	private static final Log LOG = LoggerFactory.make();

	private final FieldBridge CALENDAR_YEAR;
	private final FieldBridge CALENDAR_MONTH;
	private final FieldBridge CALENDAR_DAY;
	private final FieldBridge CALENDAR_HOUR;
	private final FieldBridge CALENDAR_MINUTE;
	private final FieldBridge CALENDAR_SECOND;
	public final FieldBridge CALENDAR_MILLISECOND;


	public CalendarBridgeProvider() {
		CALENDAR_YEAR = new TwoWayString2FieldBridgeAdaptor( CalendarBridge.CALENDAR_YEAR );
		CALENDAR_MONTH = new TwoWayString2FieldBridgeAdaptor( CalendarBridge.CALENDAR_MONTH );
		CALENDAR_DAY = new TwoWayString2FieldBridgeAdaptor( CalendarBridge.CALENDAR_DAY );
		CALENDAR_HOUR = new TwoWayString2FieldBridgeAdaptor( CalendarBridge.CALENDAR_HOUR );
		CALENDAR_MINUTE = new TwoWayString2FieldBridgeAdaptor( CalendarBridge.CALENDAR_MINUTE );
		CALENDAR_SECOND = new TwoWayString2FieldBridgeAdaptor( CalendarBridge.CALENDAR_SECOND );
		CALENDAR_MILLISECOND = new TwoWayString2FieldBridgeAdaptor( CalendarBridge.CALENDAR_MILLISECOND );
	}

	@Override
	public FieldBridge returnFieldBridgeIfMatching(Class<?> returnType, AnnotatedElement bridgedElement, ServiceManager serviceManager) {
		if ( bridgedElement.isAnnotationPresent( org.hibernate.search.annotations.CalendarBridge.class ) ) {
			Resolution resolution = bridgedElement.getAnnotation( org.hibernate.search.annotations.CalendarBridge.class )
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
