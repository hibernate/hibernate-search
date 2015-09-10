/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.bridge.impl;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Period;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.builtin.impl.TwoWayString2FieldBridgeAdaptor;
import org.hibernate.search.bridge.builtin.time.impl.DurationBridge;
import org.hibernate.search.bridge.builtin.time.impl.InstantBridge;
import org.hibernate.search.bridge.builtin.time.impl.LocalDateBridge;
import org.hibernate.search.bridge.builtin.time.impl.LocalDateTimeBridge;
import org.hibernate.search.bridge.builtin.time.impl.LocalTimeBridge;
import org.hibernate.search.bridge.builtin.time.impl.MonthDayBridge;
import org.hibernate.search.bridge.builtin.time.impl.OffsetDateTimeBridge;
import org.hibernate.search.bridge.builtin.time.impl.OffsetTimeBridge;
import org.hibernate.search.bridge.builtin.time.impl.PeriodBridge;
import org.hibernate.search.bridge.builtin.time.impl.YearBridge;
import org.hibernate.search.bridge.builtin.time.impl.YearMonthBridge;
import org.hibernate.search.bridge.builtin.time.impl.ZoneIdBridge;
import org.hibernate.search.bridge.builtin.time.impl.ZoneOffsetBridge;
import org.hibernate.search.bridge.builtin.time.impl.ZonedDateTimeBridge;
import org.hibernate.search.bridge.spi.BridgeProvider;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * {@link BridgeProvider} for the classes in java.time.*
 * <p>
 * Note that the bridges are created only if the specific the package java.time exists on the classpath
 *
 * @author Davide D'Alto
 */
public class JavaTimeBridgeProvider implements BridgeProvider {

	private static final Log LOG = LoggerFactory.make();

	private static final boolean ACTIVATED = JavaTimeBridgeProvider.javaTimePackageExists();

	private final Map<String, FieldBridge> builtInBridges;

	JavaTimeBridgeProvider() {
		if ( isActive() ) {
			this.builtInBridges = populateBridgeMap();
		}
		else {
			this.builtInBridges = Collections.emptyMap();
		}
	}

	private static Map<String, FieldBridge> populateBridgeMap() {
		Map<String, FieldBridge> bridges = new HashMap<String, FieldBridge>( 12 );
		bridges.put( Year.class.getName(), YearBridge.INSTANCE );
		bridges.put( YearMonth.class.getName(), new TwoWayString2FieldBridgeAdaptor( YearMonthBridge.INSTANCE ) );
		bridges.put( MonthDay.class.getName(), new TwoWayString2FieldBridgeAdaptor( MonthDayBridge.INSTANCE ) );
		bridges.put( LocalDateTime.class.getName(), new TwoWayString2FieldBridgeAdaptor( LocalDateTimeBridge.INSTANCE ) );
		bridges.put( LocalDate.class.getName(), new TwoWayString2FieldBridgeAdaptor( LocalDateBridge.INSTANCE ) );
		bridges.put( LocalTime.class.getName(), new TwoWayString2FieldBridgeAdaptor( LocalTimeBridge.INSTANCE ) );
		bridges.put( OffsetDateTime.class.getName(), new TwoWayString2FieldBridgeAdaptor( OffsetDateTimeBridge.INSTANCE ) );
		bridges.put( OffsetTime.class.getName(), new TwoWayString2FieldBridgeAdaptor( OffsetTimeBridge.INSTANCE ) );
		bridges.put( ZonedDateTime.class.getName(), new TwoWayString2FieldBridgeAdaptor( ZonedDateTimeBridge.INSTANCE ) );
		bridges.put( ZoneOffset.class.getName(), new TwoWayString2FieldBridgeAdaptor( ZoneOffsetBridge.INSTANCE ) );
		bridges.put( ZoneId.class.getName(), new TwoWayString2FieldBridgeAdaptor( ZoneIdBridge.INSTANCE ) );
		bridges.put( Period.class.getName(), new TwoWayString2FieldBridgeAdaptor( PeriodBridge.INSTANCE ) );
		bridges.put( Duration.class.getName(), DurationBridge.INSTANCE );
		bridges.put( Instant.class.getName(), InstantBridge.INSTANCE );
		return bridges;
	}

	public static boolean isActive() {
		return ACTIVATED;
	}

	private static boolean javaTimePackageExists() {
		try {
			Class.forName( "java.time.LocalDate" );
			return true;
		}
		catch (ClassNotFoundException e) {
			LOG.javaTimeBridgeWontBeAdded( e );
			return false;
		}
	}

	@Override
	public FieldBridge provideFieldBridge(BridgeProviderContext bridgeProviderContext) {
		return builtInBridges.get( bridgeProviderContext.getReturnType().getName() );
	}
}
