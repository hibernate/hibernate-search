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
import org.hibernate.search.engine.service.classloading.spi.ClassLoaderService;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * {@link BridgeProvider} for the classes in java.time.*
 * <p>
 * The bridge for a specific type is created only if the type get found using a {@link ClassLoaderService}.
 *
 * @author Davide D'Alto
 */
class JavaTimeBridgeProvider implements BridgeProvider {

	private static final Log LOG = LoggerFactory.make();

	private final Map<String, FieldBridge> builtInBridges;

	JavaTimeBridgeProvider(ClassLoaderService classLoaderService) {
		Map<String, FieldBridge> bridges = populateBridgeMap( classLoaderService );
		this.builtInBridges = bridges;
	}

	private static Map<String, FieldBridge> populateBridgeMap(ClassLoaderService classLoaderService) {
		Map<String, FieldBridge> bridges = new HashMap<String, FieldBridge>( 12 );
		if ( classExists( classLoaderService, "java.time.Year" ) ) {
			bridges.put( Year.class.getName(), YearBridge.INSTANCE );
		}
		if ( classExists( classLoaderService, "java.time.YearMonth" ) ) {
			bridges.put( YearMonth.class.getName(), new TwoWayString2FieldBridgeAdaptor( YearMonthBridge.INSTANCE ) );
		}
		if ( classExists( classLoaderService, "java.time.MonthDay" ) ) {
			bridges.put( MonthDay.class.getName(), new TwoWayString2FieldBridgeAdaptor( MonthDayBridge.INSTANCE ) );
		}
		if ( classExists( classLoaderService, "java.time.LocalDateTime" ) ) {
			bridges.put( LocalDateTime.class.getName(), new TwoWayString2FieldBridgeAdaptor( LocalDateTimeBridge.INSTANCE ) );
		}
		if ( classExists( classLoaderService, "java.time.LocalDate" ) ) {
			bridges.put( LocalDate.class.getName(), new TwoWayString2FieldBridgeAdaptor( LocalDateBridge.INSTANCE ) );
		}
		if ( classExists( classLoaderService, "java.time.LocalTime" ) ) {
			bridges.put( LocalTime.class.getName(), new TwoWayString2FieldBridgeAdaptor( LocalTimeBridge.INSTANCE ) );
		}
		if ( classExists( classLoaderService, "java.time.OffsetDateTime" ) ) {
			bridges.put( OffsetDateTime.class.getName(), new TwoWayString2FieldBridgeAdaptor( OffsetDateTimeBridge.INSTANCE ) );
		}
		if ( classExists( classLoaderService, "java.time.OffsetTime" ) ) {
			bridges.put( OffsetTime.class.getName(), new TwoWayString2FieldBridgeAdaptor( OffsetTimeBridge.INSTANCE ) );
		}
		if ( classExists( classLoaderService, "java.time.ZonedDateTime" ) ) {
			bridges.put( ZonedDateTime.class.getName(), new TwoWayString2FieldBridgeAdaptor( ZonedDateTimeBridge.INSTANCE ) );
		}
		if ( classExists( classLoaderService, "java.time.ZoneOffset" ) ) {
			bridges.put( ZoneOffset.class.getName(), new TwoWayString2FieldBridgeAdaptor( ZoneOffsetBridge.INSTANCE ) );
		}
		if ( classExists( classLoaderService, "java.time.ZoneId" ) ) {
			bridges.put( ZoneId.class.getName(), new TwoWayString2FieldBridgeAdaptor( ZoneIdBridge.INSTANCE ) );
		}
		if ( classExists( classLoaderService, "java.time.Period" ) ) {
			bridges.put( Period.class.getName(), new TwoWayString2FieldBridgeAdaptor( PeriodBridge.INSTANCE ) );
		}
		if ( classExists( classLoaderService, "java.time.Duration" ) ) {
			bridges.put( Duration.class.getName(), DurationBridge.INSTANCE );
		}
		if ( classExists( classLoaderService, "java.time.Instant" ) ) {
			bridges.put( Instant.class.getName(), InstantBridge.INSTANCE );
		}
		if ( bridges.isEmpty() ) {
			bridges = Collections.emptyMap();
		}
		return bridges;
	}

	/**
	 * @return {@code true} if at least one of the supported classes in the package java.time exists on the classpath,
	 * {@code false} otherwise.
	 */
	public boolean hasFoundSomeJavaTimeTypes() {
		return !builtInBridges.isEmpty();
	}

	private static boolean classExists(ClassLoaderService classLoaderService, String className) {
		try {
			classLoaderService.classForName( className );
			return true;
		}
		catch (org.hibernate.search.engine.service.classloading.spi.ClassLoadingException e) {
			LOG.javaTimeBridgeWontBeAdded( className );
			return false;
		}
	}

	@Override
	public FieldBridge provideFieldBridge(BridgeProviderContext bridgeProviderContext) {
		return builtInBridges.get( bridgeProviderContext.getReturnType().getName() );
	}
}
