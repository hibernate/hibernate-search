/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.bridge.impl;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.TwoWayFieldBridge;
import org.hibernate.search.bridge.builtin.impl.TwoWayString2FieldBridgeAdaptor;
import org.hibernate.search.bridge.builtin.time.impl.InstantNumericBridge;
import org.hibernate.search.bridge.builtin.time.impl.LocalDateNumericBridge;
import org.hibernate.search.bridge.builtin.time.impl.LocalDateTimeNumericBridge;
import org.hibernate.search.bridge.builtin.time.impl.LocalTimeNumericBridge;
import org.hibernate.search.bridge.builtin.time.impl.MonthDayNumericBridge;
import org.hibernate.search.bridge.builtin.time.impl.YearMonthNumericBridge;
import org.hibernate.search.bridge.builtin.time.impl.YearNumericBridge;
import org.hibernate.search.bridge.builtin.time.impl.ZoneIdStringBridge;
import org.hibernate.search.bridge.builtin.time.impl.ZoneOffseStringBridge;
import org.hibernate.search.bridge.spi.BridgeProvider;

/**
 * {@link BridgeProvider} for the classes in java.time.*
 *
 * @author Davide D'Alto
 */
class JavaTimeBridgeProvider implements BridgeProvider {

	private static final TwoWayFieldBridge ZONE_OFFSET = new TwoWayString2FieldBridgeAdaptor( new ZoneOffseStringBridge() );
	private static final TwoWayFieldBridge ZONE_ID = new TwoWayString2FieldBridgeAdaptor( new ZoneIdStringBridge() );

	private final Map<String, FieldBridge> builtInBridges;

	JavaTimeBridgeProvider() {
		Map<String, FieldBridge> bridges = new HashMap<String, FieldBridge>();
		bridges.put( MonthDay.class.getName(), MonthDayNumericBridge.INSTANCE );
		bridges.put( Year.class.getName(), YearNumericBridge.INSTANCE );
		bridges.put( YearMonth.class.getName(), YearMonthNumericBridge.INSTANCE );
		bridges.put( Instant.class.getName(), InstantNumericBridge.INSTANCE );
		bridges.put( LocalDate.class.getName(), LocalDateNumericBridge.INSTANCE );
		bridges.put( LocalDateTime.class.getName(), LocalDateTimeNumericBridge.INSTANCE );
		bridges.put( LocalTime.class.getName(), LocalTimeNumericBridge.INSTANCE );
		bridges.put( ZoneOffset.class.getName(), ZONE_OFFSET );
		bridges.put( ZoneId.class.getName(), ZONE_ID );
		this.builtInBridges = bridges;
	}

	@Override
	public FieldBridge provideFieldBridge(BridgeProviderContext bridgeProviderContext) {
		return builtInBridges.get( bridgeProviderContext.getReturnType().getName() );
	}
}
