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
import org.hibernate.search.engine.environment.bean.BeanHolder;
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
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultUUIDIdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultUUIDValueBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultZoneIdValueBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultZoneOffsetValueBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.PassThroughValueBridge;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.IdentifierBridgeBuilder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.ValueBridgeBuilder;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.model.spi.PojoGenericTypeModel;
import org.hibernate.search.mapper.pojo.model.typepattern.impl.TypePatternMatcher;
import org.hibernate.search.mapper.pojo.model.typepattern.impl.TypePatternMatcherFactory;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;


public final class BridgeResolver {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final Map<Class<?>, IdentifierBridgeBuilder> exactRawTypeIdentifierBridgeMappings = new HashMap<>();
	private final Map<Class<?>, ValueBridgeBuilder> exactRawTypeValueBridgeMappings = new HashMap<>();

	private final List<TypePatternBridgeMapping<IdentifierBridgeBuilder>> typePatternIdentifierBridgeMappings = new ArrayList<>();
	private final List<TypePatternBridgeMapping<ValueBridgeBuilder>> typePatternValueBridgeMappings = new ArrayList<>();

	public BridgeResolver(TypePatternMatcherFactory typePatternMatcherFactory) {
		// TODO HSEARCH-3096 add an extension point to override these maps, or at least to add defaults for other types

		TypePatternMatcher concreteEnumPattern = typePatternMatcherFactory.createRawSuperTypeMatcher( Enum.class )
				.and( typePatternMatcherFactory.createExactRawTypeMatcher( Enum.class ).negate() );

		addIdentifierBridgeForExactRawType( Integer.class, ignored -> BeanHolder.of( new DefaultIntegerIdentifierBridge() ) );
		addIdentifierBridgeForExactRawType( Long.class, ignored -> BeanHolder.of( new DefaultLongIdentifierBridge() ) );
		addIdentifierBridgeForTypePattern( concreteEnumPattern, ignored -> BeanHolder.of( new DefaultEnumIdentifierBridge<>() ) );
		addIdentifierBridgeForExactRawType( Short.class, ignored -> BeanHolder.of( new DefaultShortIdentifierBridge() ) );
		addIdentifierBridgeForExactRawType( BigInteger.class, ignored -> BeanHolder.of( new DefaultBigIntegerIdentifierBridge() ) );
		addIdentifierBridgeForExactRawType( UUID.class, ignored -> BeanHolder.of( new DefaultUUIDIdentifierBridge() ) );

		addValueBridgeForExactRawType( Integer.class, new PassThroughValueBridge.Builder<>( Integer.class, ConvertUtils::convertInteger ) );
		addValueBridgeForExactRawType( Long.class, new PassThroughValueBridge.Builder<>( Long.class, ConvertUtils::convertLong ) );
		addValueBridgeForExactRawType( Boolean.class, new PassThroughValueBridge.Builder<>( Boolean.class, ConvertUtils::convertBoolean ) );
		addValueBridgeForExactRawType( String.class, new PassThroughValueBridge.Builder<>( String.class, ParseUtils::parseString ) );
		addValueBridgeForExactRawType( LocalDate.class, new PassThroughValueBridge.Builder<>( LocalDate.class, ParseUtils::parseLocalDate ) );
		addValueBridgeForExactRawType( Instant.class, new PassThroughValueBridge.Builder<>( Instant.class, ParseUtils::parseInstant ) );
		addValueBridgeForExactRawType( Date.class, new DefaultJavaUtilDateValueBridge() );
		addValueBridgeForExactRawType( Calendar.class, new DefaultJavaUtilCalendarValueBridge() );
		addValueBridgeForTypePattern( concreteEnumPattern, new DefaultEnumValueBridge.Builder() );
		addValueBridgeForExactRawType( Character.class, new DefaultCharacterValueBridge() );
		addValueBridgeForExactRawType( Byte.class, new PassThroughValueBridge.Builder<>( Byte.class, ConvertUtils::convertByte ) );
		addValueBridgeForExactRawType( Short.class, new PassThroughValueBridge.Builder<>( Short.class, ConvertUtils::convertShort ) );
		addValueBridgeForExactRawType( Float.class, new PassThroughValueBridge.Builder<>( Float.class, ConvertUtils::convertFloat ) );
		addValueBridgeForExactRawType( Double.class, new PassThroughValueBridge.Builder<>( Double.class, ConvertUtils::convertDouble ) );
		addValueBridgeForExactRawType( BigDecimal.class, new PassThroughValueBridge.Builder<>( BigDecimal.class, ConvertUtils::convertBigDecimal ) );
		addValueBridgeForExactRawType( BigInteger.class, new PassThroughValueBridge.Builder<>( BigInteger.class, ConvertUtils::convertBigInteger ) );
		addValueBridgeForExactRawType( UUID.class, new DefaultUUIDValueBridge() );
		addValueBridgeForExactRawType( LocalDateTime.class, new PassThroughValueBridge.Builder<>( LocalDateTime.class, ParseUtils::parseLocalDateTime ) );
		addValueBridgeForExactRawType( LocalTime.class, new PassThroughValueBridge.Builder<>( LocalTime.class, ParseUtils::parseLocalTime ) );
		addValueBridgeForExactRawType( ZonedDateTime.class, new PassThroughValueBridge.Builder<>( ZonedDateTime.class, ParseUtils::parseZonedDateTime ) );
		addValueBridgeForExactRawType( Year.class, new PassThroughValueBridge.Builder<>( Year.class, ParseUtils::parseYear ) );
		addValueBridgeForExactRawType( YearMonth.class, new PassThroughValueBridge.Builder<>( YearMonth.class, ParseUtils::parseYearMonth ) );
		addValueBridgeForExactRawType( MonthDay.class, new PassThroughValueBridge.Builder<>( MonthDay.class, ParseUtils::parseMonthDay ) );
		addValueBridgeForExactRawType( OffsetDateTime.class, new PassThroughValueBridge.Builder<>( OffsetDateTime.class, ParseUtils::parseOffsetDateTime ) );
		addValueBridgeForExactRawType( OffsetTime.class, new PassThroughValueBridge.Builder<>( OffsetTime.class, ParseUtils::parseOffsetTime ) );
		addValueBridgeForExactRawType( ZoneOffset.class, new DefaultZoneOffsetValueBridge() );
		addValueBridgeForExactRawType( ZoneId.class, new DefaultZoneIdValueBridge() );
		addValueBridgeForExactRawType( Period.class, new DefaultPeriodValueBridge() );
		addValueBridgeForExactRawType( Duration.class, new DefaultDurationValueBridge() );
		addValueBridgeForExactRawType( URI.class, new DefaultJavaNetURIValueBridge() );
		addValueBridgeForExactRawType( URL.class, new DefaultJavaNetURLValueBridge() );
		addValueBridgeForExactRawType( java.sql.Date.class, new DefaultJavaSqlDateValueBridge() );
		addValueBridgeForExactRawType( Timestamp.class, new DefaultJavaSqlTimestampValueBridge() );
		addValueBridgeForExactRawType( Time.class, new DefaultJavaSqlTimeValueBridge() );
	}

	public IdentifierBridgeBuilder resolveIdentifierBridgeForType(PojoGenericTypeModel<?> sourceType) {
		IdentifierBridgeBuilder result = getBridgeBuilderOrNull(
				sourceType,
				exactRawTypeIdentifierBridgeMappings,
				typePatternIdentifierBridgeMappings
		);
		if ( result == null ) {
			throw log.unableToResolveDefaultIdentifierBridgeFromSourceType( sourceType );
		}
		return result;
	}

	public ValueBridgeBuilder resolveValueBridgeForType(PojoGenericTypeModel<?> sourceType) {
		ValueBridgeBuilder result = getBridgeBuilderOrNull(
				sourceType,
				exactRawTypeValueBridgeMappings,
				typePatternValueBridgeMappings
		);
		if ( result == null ) {
			throw log.unableToResolveDefaultValueBridgeFromSourceType( sourceType );
		}
		return result;
	}

	private <I> void addIdentifierBridgeForExactRawType(Class<I> type, IdentifierBridgeBuilder builder) {
		exactRawTypeIdentifierBridgeMappings.put( type, builder );
	}

	private void addIdentifierBridgeForTypePattern(TypePatternMatcher typePatternMatcher,
			IdentifierBridgeBuilder builder) {
		typePatternIdentifierBridgeMappings.add( new TypePatternBridgeMapping<>( typePatternMatcher, builder ) );
	}

	private <V> void addValueBridgeForExactRawType(Class<V> type, ValueBridgeBuilder builder) {
		exactRawTypeValueBridgeMappings.put( type, builder );
	}

	private <V> void addValueBridgeForExactRawType(Class<V> type, ValueBridge<V, ?> bridge) {
		addValueBridgeForExactRawType( type, context -> context.setBridge( type, bridge ) );
	}

	private void addValueBridgeForTypePattern(TypePatternMatcher typePatternMatcher,
			ValueBridgeBuilder builder) {
		typePatternValueBridgeMappings.add( new TypePatternBridgeMapping<>( typePatternMatcher, builder ) );
	}

	private static <B> B getBridgeBuilderOrNull(PojoGenericTypeModel<?> sourceType,
			Map<Class<?>, B> exactRawTypeBridgeMappings,
			List<TypePatternBridgeMapping<B>> typePatternBridgeMappings) {
		Class<?> rawType = sourceType.getRawType().getJavaClass();
		B result = exactRawTypeBridgeMappings.get( rawType );

		if ( result == null ) {
			Iterator<TypePatternBridgeMapping<B>> mappingIterator = typePatternBridgeMappings.iterator();
			while ( result == null && mappingIterator.hasNext() ) {
				result = mappingIterator.next().getBuilderIfMatching( sourceType );
			}
		}

		return result;
	}

	private static final class TypePatternBridgeMapping<B> {
		private final TypePatternMatcher matcher;
		private final B builder;

		TypePatternBridgeMapping(TypePatternMatcher matcher, B builder) {
			this.matcher = matcher;
			this.builder = builder;
		}

		B getBuilderIfMatching(PojoGenericTypeModel<?> typeModel) {
			if ( matcher.matches( typeModel ) ) {
				return builder;
			}
			else {
				return null;
			}
		}
	}

}
