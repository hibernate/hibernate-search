/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.elasticsearch.bridge.impl;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.util.Map;

import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.spi.BridgeProvider;
import org.hibernate.search.bridge.util.impl.TwoWayString2FieldBridgeIgnoreAnalyzerAdaptor;
import org.hibernate.search.elasticsearch.bridge.builtin.time.impl.ElasticsearchInstantBridge;
import org.hibernate.search.elasticsearch.bridge.builtin.time.impl.ElasticsearchLocalDateBridge;
import org.hibernate.search.elasticsearch.bridge.builtin.time.impl.ElasticsearchLocalDateTimeBridge;
import org.hibernate.search.elasticsearch.bridge.builtin.time.impl.ElasticsearchLocalTimeBridge;
import org.hibernate.search.elasticsearch.bridge.builtin.time.impl.ElasticsearchMonthDayBridge;
import org.hibernate.search.elasticsearch.bridge.builtin.time.impl.ElasticsearchOffsetDateTimeBridge;
import org.hibernate.search.elasticsearch.bridge.builtin.time.impl.ElasticsearchOffsetTimeBridge;
import org.hibernate.search.elasticsearch.bridge.builtin.time.impl.ElasticsearchYearBridge;
import org.hibernate.search.elasticsearch.bridge.builtin.time.impl.ElasticsearchYearMonthBridge;
import org.hibernate.search.elasticsearch.bridge.builtin.time.impl.ElasticsearchZonedDateTimeBridge;
import org.hibernate.search.util.impl.CollectionHelper;

/**
 * Creates bridges specific to Elasticsearch for the classes in java.time.*
 * <p>
 * Note that the bridges are created only if the specific package java.time exists on the classpath
 *
 * @author Davide D'Alto
 * @author Yoann Rodiere
 */
class ElasticsearchJavaTimeBridgeProvider implements BridgeProvider {

	private final Map<String, FieldBridge> builtInBridges;

	ElasticsearchJavaTimeBridgeProvider() {
		this.builtInBridges = populateBridgeMap();
	}

	private static Map<String, FieldBridge> populateBridgeMap() {
		Map<String, FieldBridge> bridges = CollectionHelper.newHashMap( 10 );
		bridges.put( Year.class.getName(), new TwoWayString2FieldBridgeIgnoreAnalyzerAdaptor( ElasticsearchYearBridge.INSTANCE ) );
		bridges.put( YearMonth.class.getName(), new TwoWayString2FieldBridgeIgnoreAnalyzerAdaptor( ElasticsearchYearMonthBridge.INSTANCE ) );
		bridges.put( MonthDay.class.getName(), new TwoWayString2FieldBridgeIgnoreAnalyzerAdaptor( ElasticsearchMonthDayBridge.INSTANCE ) );
		bridges.put( LocalDateTime.class.getName(), new TwoWayString2FieldBridgeIgnoreAnalyzerAdaptor( ElasticsearchLocalDateTimeBridge.INSTANCE ) );
		bridges.put( LocalDate.class.getName(), new TwoWayString2FieldBridgeIgnoreAnalyzerAdaptor( ElasticsearchLocalDateBridge.INSTANCE ) );
		bridges.put( LocalTime.class.getName(), new TwoWayString2FieldBridgeIgnoreAnalyzerAdaptor( ElasticsearchLocalTimeBridge.INSTANCE ) );
		bridges.put( Instant.class.getName(), new TwoWayString2FieldBridgeIgnoreAnalyzerAdaptor( ElasticsearchInstantBridge.INSTANCE ) );
		bridges.put( OffsetDateTime.class.getName(), new TwoWayString2FieldBridgeIgnoreAnalyzerAdaptor( ElasticsearchOffsetDateTimeBridge.INSTANCE ) );
		bridges.put( OffsetTime.class.getName(), new TwoWayString2FieldBridgeIgnoreAnalyzerAdaptor( ElasticsearchOffsetTimeBridge.INSTANCE ) );
		bridges.put( ZonedDateTime.class.getName(), new TwoWayString2FieldBridgeIgnoreAnalyzerAdaptor( ElasticsearchZonedDateTimeBridge.INSTANCE ) );

		/*
		 * Use the default Lucene bridges for ZoneOffset, ZoneId, Period and Duration
		 */

		return bridges;
	}

	@Override
	public FieldBridge provideFieldBridge(BridgeProviderContext bridgeProviderContext) {
		return builtInBridges.get( bridgeProviderContext.getReturnType().getName() );
	}
}
