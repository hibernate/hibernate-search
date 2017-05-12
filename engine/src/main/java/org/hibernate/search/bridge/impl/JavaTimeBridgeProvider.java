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
import java.util.Map;

import org.hibernate.search.bridge.FieldBridge;
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
import org.hibernate.search.bridge.util.impl.TwoWayString2FieldBridgeIgnoreAnalyzerAdaptor;
import org.hibernate.search.util.impl.CollectionHelper;

/**
 * {@link BridgeProvider} for the classes in java.time.*
 *
 * @author Davide D'Alto
 */
class JavaTimeBridgeProvider implements BridgeProvider {

	private final Map<String, FieldBridge> builtInBridges;

	JavaTimeBridgeProvider() {
		this.builtInBridges = populateBridgeMap();
	}

	private static Map<String, FieldBridge> populateBridgeMap() {
		Map<String, FieldBridge> bridges = CollectionHelper.newHashMap( 14 );
		bridges.put( Year.class.getName(), YearBridge.INSTANCE );
		bridges.put( YearMonth.class.getName(), new TwoWayString2FieldBridgeIgnoreAnalyzerAdaptor( YearMonthBridge.INSTANCE ) );
		bridges.put( MonthDay.class.getName(), new TwoWayString2FieldBridgeIgnoreAnalyzerAdaptor( MonthDayBridge.INSTANCE ) );
		bridges.put( LocalDateTime.class.getName(), new TwoWayString2FieldBridgeIgnoreAnalyzerAdaptor( LocalDateTimeBridge.INSTANCE ) );
		bridges.put( LocalDate.class.getName(), new TwoWayString2FieldBridgeIgnoreAnalyzerAdaptor( LocalDateBridge.INSTANCE ) );
		bridges.put( LocalTime.class.getName(), new TwoWayString2FieldBridgeIgnoreAnalyzerAdaptor( LocalTimeBridge.INSTANCE ) );
		bridges.put( OffsetDateTime.class.getName(), new TwoWayString2FieldBridgeIgnoreAnalyzerAdaptor( OffsetDateTimeBridge.INSTANCE ) );
		bridges.put( OffsetTime.class.getName(), new TwoWayString2FieldBridgeIgnoreAnalyzerAdaptor( OffsetTimeBridge.INSTANCE ) );
		bridges.put( ZonedDateTime.class.getName(), new TwoWayString2FieldBridgeIgnoreAnalyzerAdaptor( ZonedDateTimeBridge.INSTANCE ) );
		bridges.put( ZoneOffset.class.getName(), new TwoWayString2FieldBridgeIgnoreAnalyzerAdaptor( ZoneOffsetBridge.INSTANCE ) );
		bridges.put( ZoneId.class.getName(), new TwoWayString2FieldBridgeIgnoreAnalyzerAdaptor( ZoneIdBridge.INSTANCE ) );
		bridges.put( Period.class.getName(), new TwoWayString2FieldBridgeIgnoreAnalyzerAdaptor( PeriodBridge.INSTANCE ) );
		bridges.put( Duration.class.getName(), DurationBridge.INSTANCE );
		bridges.put( Instant.class.getName(), InstantBridge.INSTANCE );
		return bridges;
	}

	@Override
	public FieldBridge provideFieldBridge(BridgeProviderContext bridgeProviderContext) {
		return builtInBridges.get( bridgeProviderContext.getReturnType().getName() );
	}
}
