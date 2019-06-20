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
import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
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
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.BridgeBuilder;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.model.spi.PojoGenericTypeModel;
import org.hibernate.search.mapper.pojo.model.typepattern.impl.TypePatternMatcher;
import org.hibernate.search.mapper.pojo.model.typepattern.impl.TypePatternMatcherFactory;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;


public final class BridgeResolver {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final Map<Class<?>, BridgeBuilder<? extends IdentifierBridge<?>>> exactRawTypeIdentifierBridgeMappings = new HashMap<>();
	private final Map<Class<?>, BridgeBuilder<? extends ValueBridge<?, ?>>> exactRawTypeValueBridgeMappings = new HashMap<>();

	private final List<TypePatternBridgeMapping<? extends IdentifierBridge<?>>> typePatternIdentifierBridgeMappings = new ArrayList<>();
	private final List<TypePatternBridgeMapping<? extends ValueBridge<?, ?>>> typePatternValueBridgeMappings = new ArrayList<>();

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

		addValueBridgeForExactRawType( Integer.class, ignored -> BeanHolder.of( new PassThroughValueBridge<>( Integer.class, ConvertUtils::convertInteger ) ) );
		addValueBridgeForExactRawType( Long.class, ignored -> BeanHolder.of( new PassThroughValueBridge<>( Long.class, ConvertUtils::convertLong ) ) );
		addValueBridgeForExactRawType( Boolean.class, ignored -> BeanHolder.of( new PassThroughValueBridge<>( Boolean.class, ConvertUtils::convertBoolean ) ) );
		addValueBridgeForExactRawType( String.class, ignored -> BeanHolder.of( new PassThroughValueBridge<>( String.class, ParseUtils::parseString ) ) );
		addValueBridgeForExactRawType( LocalDate.class, ignored -> BeanHolder.of( new PassThroughValueBridge<>( LocalDate.class, ParseUtils::parseLocalDate ) ) );
		addValueBridgeForExactRawType( Instant.class, ignored -> BeanHolder.of( new PassThroughValueBridge<>( Instant.class, ParseUtils::parseInstant ) ) );
		addValueBridgeForExactRawType( Date.class, ignored -> BeanHolder.of( new DefaultJavaUtilDateValueBridge() ) );
		addValueBridgeForExactRawType( Calendar.class, ignored -> BeanHolder.of( new DefaultJavaUtilCalendarValueBridge() ) );
		addValueBridgeForTypePattern( concreteEnumPattern, ignored -> BeanHolder.of( new DefaultEnumValueBridge<>() ) );
		addValueBridgeForExactRawType( Character.class, ignored -> BeanHolder.of( new DefaultCharacterValueBridge() ) );
		addValueBridgeForExactRawType( Byte.class, ignored -> BeanHolder.of( new PassThroughValueBridge<>( Byte.class, ConvertUtils::convertByte ) ) );
		addValueBridgeForExactRawType( Short.class, ignored -> BeanHolder.of( new PassThroughValueBridge<>( Short.class, ConvertUtils::convertShort ) ) );
		addValueBridgeForExactRawType( Float.class, ignored -> BeanHolder.of( new PassThroughValueBridge<>( Float.class, ConvertUtils::convertFloat ) ) );
		addValueBridgeForExactRawType( Double.class, ignored -> BeanHolder.of( new PassThroughValueBridge<>( Double.class, ConvertUtils::convertDouble ) ) );
		addValueBridgeForExactRawType( BigDecimal.class, ignored -> BeanHolder.of( new PassThroughValueBridge<>( BigDecimal.class, ConvertUtils::convertBigDecimal ) ) );
		addValueBridgeForExactRawType( BigInteger.class, ignored -> BeanHolder.of( new PassThroughValueBridge<>( BigInteger.class, ConvertUtils::convertBigInteger ) ) );
		addValueBridgeForExactRawType( UUID.class, ignored -> BeanHolder.of( new DefaultUUIDValueBridge() ) );
		addValueBridgeForExactRawType( LocalDateTime.class, ignored -> BeanHolder.of( new PassThroughValueBridge<>( LocalDateTime.class, ParseUtils::parseLocalDateTime ) ) );
		addValueBridgeForExactRawType( LocalTime.class, ignored -> BeanHolder.of( new PassThroughValueBridge<>( LocalTime.class, ParseUtils::parseLocalTime ) ) );
		addValueBridgeForExactRawType( ZonedDateTime.class, ignored -> BeanHolder.of( new PassThroughValueBridge<>( ZonedDateTime.class, ParseUtils::parseZonedDateTime ) ) );
		addValueBridgeForExactRawType( Year.class, ignored -> BeanHolder.of( new PassThroughValueBridge<>( Year.class, ParseUtils::parseYear ) ) );
		addValueBridgeForExactRawType( YearMonth.class, ignored -> BeanHolder.of( new PassThroughValueBridge<>( YearMonth.class, ParseUtils::parseYearMonth ) ) );
		addValueBridgeForExactRawType( MonthDay.class, ignored -> BeanHolder.of( new PassThroughValueBridge<>( MonthDay.class, ParseUtils::parseMonthDay ) ) );
		addValueBridgeForExactRawType( OffsetDateTime.class, ignored -> BeanHolder.of( new PassThroughValueBridge<>( OffsetDateTime.class, ParseUtils::parseOffsetDateTime ) ) );
		addValueBridgeForExactRawType( OffsetTime.class, ignored -> BeanHolder.of( new PassThroughValueBridge<>( OffsetTime.class, ParseUtils::parseOffsetTime ) ) );
		addValueBridgeForExactRawType( ZoneOffset.class, ignored -> BeanHolder.of( new DefaultZoneOffsetValueBridge() ) );
		addValueBridgeForExactRawType( ZoneId.class, ignored -> BeanHolder.of( new DefaultZoneIdValueBridge() ) );
		addValueBridgeForExactRawType( Period.class, ignored -> BeanHolder.of( new DefaultPeriodValueBridge() ) );
		addValueBridgeForExactRawType( Duration.class, ignored -> BeanHolder.of( new DefaultDurationValueBridge() ) );
		addValueBridgeForExactRawType( URI.class, ignored -> BeanHolder.of( new DefaultJavaNetURIValueBridge() ) );
		addValueBridgeForExactRawType( URL.class, ignored -> BeanHolder.of( new DefaultJavaNetURLValueBridge() ) );
		addValueBridgeForExactRawType( java.sql.Date.class, ignored -> BeanHolder.of( new DefaultJavaSqlDateValueBridge() ) );
		addValueBridgeForExactRawType( Timestamp.class, ignored -> BeanHolder.of( new DefaultJavaSqlTimestampValueBridge() ) );
		addValueBridgeForExactRawType( Time.class, ignored -> BeanHolder.of( new DefaultJavaSqlTimeValueBridge() ) );
	}

	public BridgeBuilder<? extends IdentifierBridge<?>> resolveIdentifierBridgeForType(PojoGenericTypeModel<?> sourceType) {
		BridgeBuilder<? extends IdentifierBridge<?>> result = getBridgeBuilderOrNull(
				sourceType,
				exactRawTypeIdentifierBridgeMappings,
				typePatternIdentifierBridgeMappings
		);
		if ( result == null ) {
			throw log.unableToResolveDefaultIdentifierBridgeFromSourceType( sourceType );
		}
		return result;
	}

	public BridgeBuilder<? extends ValueBridge<?, ?>> resolveValueBridgeForType(PojoGenericTypeModel<?> sourceType) {
		BridgeBuilder<? extends ValueBridge<?, ?>> result = getBridgeBuilderOrNull(
				sourceType,
				exactRawTypeValueBridgeMappings,
				typePatternValueBridgeMappings
		);
		if ( result == null ) {
			throw log.unableToResolveDefaultValueBridgeFromSourceType( sourceType );
		}
		return result;
	}

	private <I> void addIdentifierBridgeForExactRawType(Class<I> type, BridgeBuilder<? extends IdentifierBridge<I>> builder) {
		exactRawTypeIdentifierBridgeMappings.put( type, builder );
	}

	private void addIdentifierBridgeForTypePattern(TypePatternMatcher typePatternMatcher,
			BridgeBuilder<? extends IdentifierBridge<?>> builder) {
		typePatternIdentifierBridgeMappings.add( new TypePatternBridgeMapping<>( typePatternMatcher, builder ) );
	}

	private <V> void addValueBridgeForExactRawType(Class<V> type, BridgeBuilder<? extends ValueBridge<? super V, ?>> builder) {
		exactRawTypeValueBridgeMappings.put( type, builder );
	}

	private void addValueBridgeForTypePattern(TypePatternMatcher typePatternMatcher,
			BridgeBuilder<? extends ValueBridge<?, ?>> builder) {
		typePatternValueBridgeMappings.add( new TypePatternBridgeMapping<>( typePatternMatcher, builder ) );
	}

	private static <B> BridgeBuilder<? extends B> getBridgeBuilderOrNull(PojoGenericTypeModel<?> sourceType,
			Map<Class<?>, BridgeBuilder<? extends B>> exactRawTypeBridgeMappings,
			List<TypePatternBridgeMapping<? extends B>> typePatternBridgeMappings
	) {
		Class<?> rawType = sourceType.getRawType().getJavaClass();
		BridgeBuilder<? extends B> result = exactRawTypeBridgeMappings.get( rawType );

		if ( result == null ) {
			Iterator<TypePatternBridgeMapping<? extends B>> mappingIterator = typePatternBridgeMappings.iterator();
			while ( result == null && mappingIterator.hasNext() ) {
				result = mappingIterator.next().getBuilderIfMatching( sourceType );
			}
		}

		return result;
	}

	private static final class TypePatternBridgeMapping<B> {
		private final TypePatternMatcher matcher;
		private final BridgeBuilder<B> builder;

		TypePatternBridgeMapping(TypePatternMatcher matcher, BridgeBuilder<B> builder) {
			this.matcher = matcher;
			this.builder = builder;
		}

		BridgeBuilder<B> getBuilderIfMatching(PojoGenericTypeModel<?> typeModel) {
			if ( matcher.matches( typeModel ) ) {
				return builder;
			}
			else {
				return null;
			}
		}
	}

}
