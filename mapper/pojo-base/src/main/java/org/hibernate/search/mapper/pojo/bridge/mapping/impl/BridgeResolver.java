/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.mapping.impl;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.sql.Time;
import java.sql.Timestamp;
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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.hibernate.search.engine.cfg.spi.ConvertUtils;
import org.hibernate.search.engine.cfg.spi.ParseUtils;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultBigIntegerIdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultCharacterValueBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultDurationValueBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultEnumIdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultEnumValueBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultIntegerIdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultJavaNetURIValueBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultJavaNetURLValueBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultJavaSqlDateValueBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultJavaSqlTimeValueBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultJavaSqlTimestampValueBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultJavaUtilCalendarValueBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultJavaUtilDateValueBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultLongIdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultPeriodValueBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultShortIdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultStringIdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultUUIDIdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultUUIDValueBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultZoneIdValueBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultZoneOffsetValueBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.PassThroughValueBridge;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.IdentifierBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.ValueBinder;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.model.spi.PojoGenericTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.model.typepattern.impl.TypePatternMatcher;
import org.hibernate.search.mapper.pojo.model.typepattern.impl.TypePatternMatcherFactory;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;


public final class BridgeResolver {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final Map<PojoRawTypeIdentifier<?>, IdentifierBinder> exactRawTypeIdentifierBridgeMappings = new HashMap<>();
	private final Map<PojoRawTypeIdentifier<?>, ValueBinder> exactRawTypeValueBridgeMappings = new HashMap<>();

	private final List<TypePatternBinderMapping<IdentifierBinder>> typePatternIdentifierBridgeMappings = new ArrayList<>();
	private final List<TypePatternBinderMapping<ValueBinder>> typePatternValueBridgeMappings = new ArrayList<>();

	public BridgeResolver(TypePatternMatcherFactory typePatternMatcherFactory) {
		// TODO HSEARCH-3096 add an extension point to override these maps, or at least to add defaults for other types

		TypePatternMatcher concreteEnumPattern = typePatternMatcherFactory.createRawSuperTypeMatcher( Enum.class )
				.and( typePatternMatcherFactory.createExactRawTypeMatcher( Enum.class ).negate() );

		TypePatternMatcher concreteGeoPointPattern = typePatternMatcherFactory.createRawSuperTypeMatcher( GeoPoint.class );

		addIdentifierBridgeForExactRawType( String.class, new DefaultStringIdentifierBridge() );
		addIdentifierBridgeForExactRawType( Integer.class, new DefaultIntegerIdentifierBridge() );
		addIdentifierBridgeForExactRawType( Long.class, new DefaultLongIdentifierBridge() );
		addIdentifierBinderForTypePattern( concreteEnumPattern, new DefaultEnumIdentifierBridge.Binder() );
		addIdentifierBridgeForExactRawType( Short.class, new DefaultShortIdentifierBridge() );
		addIdentifierBridgeForExactRawType( BigInteger.class, new DefaultBigIntegerIdentifierBridge() );
		addIdentifierBridgeForExactRawType( UUID.class, new DefaultUUIDIdentifierBridge() );

		addValueBinderForExactRawType( Integer.class, new PassThroughValueBridge.Binder<>( Integer.class, ConvertUtils::convertInteger ) );
		addValueBinderForExactRawType( Long.class, new PassThroughValueBridge.Binder<>( Long.class, ConvertUtils::convertLong ) );
		addValueBinderForExactRawType( Boolean.class, new PassThroughValueBridge.Binder<>( Boolean.class, ConvertUtils::convertBoolean ) );
		addValueBinderForExactRawType( String.class, new PassThroughValueBridge.Binder<>( String.class, ParseUtils::parseString ) );
		addValueBinderForExactRawType( LocalDate.class, new PassThroughValueBridge.Binder<>( LocalDate.class, ParseUtils::parseLocalDate ) );
		addValueBinderForExactRawType( Instant.class, new PassThroughValueBridge.Binder<>( Instant.class, ParseUtils::parseInstant ) );
		addValueBridgeForExactRawType( Date.class, new DefaultJavaUtilDateValueBridge() );
		addValueBridgeForExactRawType( Calendar.class, new DefaultJavaUtilCalendarValueBridge() );
		addValueBinderForTypePattern( concreteEnumPattern, new DefaultEnumValueBridge.Binder() );
		addValueBridgeForExactRawType( Character.class, new DefaultCharacterValueBridge() );
		addValueBinderForExactRawType( Byte.class, new PassThroughValueBridge.Binder<>( Byte.class, ConvertUtils::convertByte ) );
		addValueBinderForExactRawType( Short.class, new PassThroughValueBridge.Binder<>( Short.class, ConvertUtils::convertShort ) );
		addValueBinderForExactRawType( Float.class, new PassThroughValueBridge.Binder<>( Float.class, ConvertUtils::convertFloat ) );
		addValueBinderForExactRawType( Double.class, new PassThroughValueBridge.Binder<>( Double.class, ConvertUtils::convertDouble ) );
		addValueBinderForExactRawType( BigDecimal.class, new PassThroughValueBridge.Binder<>( BigDecimal.class, ConvertUtils::convertBigDecimal ) );
		addValueBinderForExactRawType( BigInteger.class, new PassThroughValueBridge.Binder<>( BigInteger.class, ConvertUtils::convertBigInteger ) );
		addValueBridgeForExactRawType( UUID.class, new DefaultUUIDValueBridge() );
		addValueBinderForExactRawType( LocalDateTime.class, new PassThroughValueBridge.Binder<>( LocalDateTime.class, ParseUtils::parseLocalDateTime ) );
		addValueBinderForExactRawType( LocalTime.class, new PassThroughValueBridge.Binder<>( LocalTime.class, ParseUtils::parseLocalTime ) );
		addValueBinderForExactRawType( ZonedDateTime.class, new PassThroughValueBridge.Binder<>( ZonedDateTime.class, ParseUtils::parseZonedDateTime ) );
		addValueBinderForExactRawType( Year.class, new PassThroughValueBridge.Binder<>( Year.class, ParseUtils::parseYear ) );
		addValueBinderForExactRawType( YearMonth.class, new PassThroughValueBridge.Binder<>( YearMonth.class, ParseUtils::parseYearMonth ) );
		addValueBinderForExactRawType( MonthDay.class, new PassThroughValueBridge.Binder<>( MonthDay.class, ParseUtils::parseMonthDay ) );
		addValueBinderForExactRawType( OffsetDateTime.class, new PassThroughValueBridge.Binder<>( OffsetDateTime.class, ParseUtils::parseOffsetDateTime ) );
		addValueBinderForExactRawType( OffsetTime.class, new PassThroughValueBridge.Binder<>( OffsetTime.class, ParseUtils::parseOffsetTime ) );
		addValueBridgeForExactRawType( ZoneOffset.class, new DefaultZoneOffsetValueBridge() );
		addValueBridgeForExactRawType( ZoneId.class, new DefaultZoneIdValueBridge() );
		addValueBridgeForExactRawType( Period.class, new DefaultPeriodValueBridge() );
		addValueBridgeForExactRawType( Duration.class, new DefaultDurationValueBridge() );
		addValueBridgeForExactRawType( URI.class, new DefaultJavaNetURIValueBridge() );
		addValueBridgeForExactRawType( URL.class, new DefaultJavaNetURLValueBridge() );
		addValueBridgeForExactRawType( java.sql.Date.class, new DefaultJavaSqlDateValueBridge() );
		addValueBridgeForExactRawType( Timestamp.class, new DefaultJavaSqlTimestampValueBridge() );
		addValueBridgeForExactRawType( Time.class, new DefaultJavaSqlTimeValueBridge() );
		addValueBinderForTypePattern( concreteGeoPointPattern, new PassThroughValueBridge.Binder<>( GeoPoint.class, ParseUtils::parseGeoPoint ) );
	}

	public IdentifierBinder resolveIdentifierBinderForType(PojoGenericTypeModel<?> sourceType) {
		IdentifierBinder result = getBinderOrNull(
				sourceType,
				exactRawTypeIdentifierBridgeMappings,
				typePatternIdentifierBridgeMappings
		);
		if ( result == null ) {
			throw log.unableToResolveDefaultIdentifierBridgeFromSourceType( sourceType );
		}
		return result;
	}

	public ValueBinder resolveValueBinderForType(PojoGenericTypeModel<?> sourceType) {
		ValueBinder result = getBinderOrNull(
				sourceType,
				exactRawTypeValueBridgeMappings,
				typePatternValueBridgeMappings
		);
		if ( result == null ) {
			throw log.unableToResolveDefaultValueBridgeFromSourceType( sourceType );
		}
		return result;
	}

	private <I> void addIdentifierBinderForExactRawType(Class<I> type, IdentifierBinder binder) {
		exactRawTypeIdentifierBridgeMappings.put( PojoRawTypeIdentifier.of( type ), binder );
	}

	private <I> void addIdentifierBridgeForExactRawType(Class<I> type, IdentifierBridge<I> bridge) {
		addIdentifierBinderForExactRawType( type, context -> context.setBridge( type, bridge ) );
	}

	private void addIdentifierBinderForTypePattern(TypePatternMatcher typePatternMatcher,
			IdentifierBinder binder) {
		typePatternIdentifierBridgeMappings.add( new TypePatternBinderMapping<>( typePatternMatcher, binder ) );
	}

	private <V> void addValueBinderForExactRawType(Class<V> type, ValueBinder binder) {
		exactRawTypeValueBridgeMappings.put( PojoRawTypeIdentifier.of( type ), binder );
	}

	private <V> void addValueBridgeForExactRawType(Class<V> type, ValueBridge<V, ?> bridge) {
		addValueBinderForExactRawType( type, context -> context.setBridge( type, bridge ) );
	}

	private void addValueBinderForTypePattern(TypePatternMatcher typePatternMatcher,
			ValueBinder binder) {
		typePatternValueBridgeMappings.add( new TypePatternBinderMapping<>( typePatternMatcher, binder ) );
	}

	private static <B> B getBinderOrNull(PojoGenericTypeModel<?> sourceType,
			Map<PojoRawTypeIdentifier<?>, B> exactRawTypeBridgeMappings,
			List<TypePatternBinderMapping<B>> typePatternBinderMappings) {
		PojoRawTypeIdentifier<?> rawType = sourceType.getRawType().getTypeIdentifier();
		B result = exactRawTypeBridgeMappings.get( rawType );

		if ( result == null ) {
			Iterator<TypePatternBinderMapping<B>> mappingIterator = typePatternBinderMappings.iterator();
			while ( result == null && mappingIterator.hasNext() ) {
				result = mappingIterator.next().getBinderIfMatching( sourceType );
			}
		}

		return result;
	}

	private static final class TypePatternBinderMapping<B> {
		private final TypePatternMatcher matcher;
		private final B binder;

		TypePatternBinderMapping(TypePatternMatcher matcher, B binder) {
			this.matcher = matcher;
			this.binder = binder;
		}

		B getBinderIfMatching(PojoGenericTypeModel<?> typeModel) {
			if ( matcher.matches( typeModel ) ) {
				return binder;
			}
			else {
				return null;
			}
		}
	}

}
