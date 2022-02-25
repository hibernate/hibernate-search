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
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultBigDecimalBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultBigIntegerBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultBooleanBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultByteBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultCharacterBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultDoubleBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultDurationBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultEnumBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultFloatBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultGeoPointBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultInstantBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultIntegerBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultJavaNetURIBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultJavaNetURLBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultJavaSqlDateBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultJavaSqlTimeBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultJavaSqlTimestampBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultJavaUtilCalendarBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultJavaUtilDateBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultLocalDateBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultLocalDateTimeBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultLocalTimeBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultLongBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultMonthDayBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultOffsetDateTimeBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultOffsetTimeBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultPeriodBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultShortBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultStringBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultUUIDBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultYearBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultYearMonthBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultZoneIdBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultZoneOffsetBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultZonedDateTimeBridge;
import org.hibernate.search.mapper.pojo.bridge.mapping.BridgesConfigurationContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.DefaultBinderDefinitionStep;
import org.hibernate.search.mapper.pojo.bridge.mapping.DefaultBridgeDefinitionStep;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.IdentifierBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.ValueBinder;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.model.spi.PojoBootstrapIntrospector;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;
import org.hibernate.search.mapper.pojo.model.typepattern.impl.TypePatternMatcher;
import org.hibernate.search.mapper.pojo.model.typepattern.impl.TypePatternMatcherFactory;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;


public final class BridgeResolver {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final Map<PojoRawTypeIdentifier<?>, IdentifierBinder> exactRawTypeIdentifierBridgeMappings;
	private final Map<PojoRawTypeIdentifier<?>, ValueBinder> exactRawTypeValueBridgeMappings;

	private final List<TypePatternBinderMapping<IdentifierBinder>> typePatternIdentifierBridgeMappings;
	private final List<TypePatternBinderMapping<ValueBinder>> typePatternValueBridgeMappings;

	private BridgeResolver(Builder builder) {
		this.exactRawTypeIdentifierBridgeMappings = new HashMap<>( builder.exactRawTypeIdentifierBridgeMappings );
		this.exactRawTypeValueBridgeMappings = new HashMap<>( builder.exactRawTypeValueBridgeMappings );
		this.typePatternIdentifierBridgeMappings = new ArrayList<>( builder.typePatternIdentifierBridgeMappings );
		this.typePatternValueBridgeMappings = new ArrayList<>( builder.typePatternValueBridgeMappings );
		// Last added patterns get priority over the previous ones: we'll try them first.
		Collections.reverse( typePatternIdentifierBridgeMappings );
		Collections.reverse( typePatternValueBridgeMappings );
	}

	public IdentifierBinder resolveIdentifierBinderForType(PojoTypeModel<?> sourceType) {
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

	public ValueBinder resolveValueBinderForType(PojoTypeModel<?> sourceType) {
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

	private static <B> B getBinderOrNull(PojoTypeModel<?> sourceType,
			Map<PojoRawTypeIdentifier<?>, B> exactRawTypeBridgeMappings,
			List<TypePatternBinderMapping<B>> typePatternBinderMappings) {
		PojoRawTypeIdentifier<?> rawType = sourceType.rawType().typeIdentifier();
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

		B getBinderIfMatching(PojoTypeModel<?> typeModel) {
			if ( matcher.matches( typeModel ) ) {
				return binder;
			}
			else {
				return null;
			}
		}
	}

	public static class Builder implements BridgesConfigurationContext {
		private final PojoBootstrapIntrospector introspector;
		private final TypePatternMatcherFactory typePatternMatcherFactory;

		private final Map<PojoRawTypeIdentifier<?>, IdentifierBinder> exactRawTypeIdentifierBridgeMappings = new HashMap<>();
		private final Map<PojoRawTypeIdentifier<?>, ValueBinder> exactRawTypeValueBridgeMappings = new HashMap<>();

		private final List<TypePatternBinderMapping<IdentifierBinder>> typePatternIdentifierBridgeMappings = new ArrayList<>();
		private final List<TypePatternBinderMapping<ValueBinder>> typePatternValueBridgeMappings = new ArrayList<>();

		public Builder(PojoBootstrapIntrospector introspector, TypePatternMatcherFactory typePatternMatcherFactory) {
			this.introspector = introspector;
			this.typePatternMatcherFactory = typePatternMatcherFactory;
			addDefaults();
		}

		@Override
		public <T> DefaultBridgeDefinitionStep<?, T> exactType(Class<T> clazz) {
			return new ExactTypeDefaultBridgeDefinitionStep<>( introspector.typeModel( clazz ).typeIdentifier() );
		}

		@Override
		public <T> DefaultBinderDefinitionStep<?> subTypesOf(Class<T> clazz) {
			TypePatternMatcher subTypesMatcher = typePatternMatcherFactory.createRawSuperTypeMatcher( clazz );
			return new TypePatternDefaultBinderDefinitionStep( subTypesMatcher );
		}

		@Override
		public <T> DefaultBinderDefinitionStep<?> strictSubTypesOf(Class<T> clazz) {
			TypePatternMatcher strictSubTypesMatcher = typePatternMatcherFactory.createRawSuperTypeMatcher( clazz )
					.and( typePatternMatcherFactory.createExactRawTypeMatcher( clazz ).negate() );
			return new TypePatternDefaultBinderDefinitionStep( strictSubTypesMatcher );
		}

		public BridgeResolver build() {
			return new BridgeResolver( this );
		}

		private void addDefaults() {
			// java.lang
			exactType( String.class )
					.valueBridge( DefaultStringBridge.INSTANCE )
					.identifierBridge( DefaultStringBridge.INSTANCE );
			exactType( Character.class )
					.valueBridge( DefaultCharacterBridge.INSTANCE )
					.identifierBridge( DefaultCharacterBridge.INSTANCE );
			exactType( Boolean.class )
					.valueBridge( DefaultBooleanBridge.INSTANCE )
					.identifierBridge( DefaultBooleanBridge.INSTANCE );
			exactType( Byte.class )
					.valueBridge( DefaultByteBridge.INSTANCE )
					.identifierBridge( DefaultByteBridge.INSTANCE );
			exactType( Short.class )
					.valueBridge( DefaultShortBridge.INSTANCE )
					.identifierBridge( DefaultShortBridge.INSTANCE );
			exactType( Integer.class )
					.valueBridge( DefaultIntegerBridge.INSTANCE )
					.identifierBridge( DefaultIntegerBridge.INSTANCE );
			exactType( Long.class )
					.valueBridge( DefaultLongBridge.INSTANCE )
					.identifierBridge( DefaultLongBridge.INSTANCE );
			exactType( Float.class )
					.valueBridge( DefaultFloatBridge.INSTANCE )
					.identifierBridge( DefaultFloatBridge.INSTANCE );
			exactType( Double.class )
					.valueBridge( DefaultDoubleBridge.INSTANCE )
					.identifierBridge( DefaultDoubleBridge.INSTANCE );
			strictSubTypesOf( Enum.class )
					.valueBinder( DefaultEnumBridge.Binder.INSTANCE )
					.identifierBinder( DefaultEnumBridge.Binder.INSTANCE );

			// java.math
			exactType( BigInteger.class )
					.valueBridge( DefaultBigIntegerBridge.INSTANCE )
					.identifierBridge( DefaultBigIntegerBridge.INSTANCE );
			exactType( BigDecimal.class )
					.valueBridge( DefaultBigDecimalBridge.INSTANCE )
					.identifierBridge( DefaultBigDecimalBridge.INSTANCE );

			// java.time
			exactType( LocalDate.class )
					.valueBridge( DefaultLocalDateBridge.INSTANCE )
					.identifierBridge( DefaultLocalDateBridge.INSTANCE );
			exactType( Instant.class )
					.valueBridge( DefaultInstantBridge.INSTANCE )
					.identifierBridge( DefaultInstantBridge.INSTANCE );
			exactType( LocalDateTime.class )
					.valueBridge( DefaultLocalDateTimeBridge.INSTANCE )
					.identifierBridge( DefaultLocalDateTimeBridge.INSTANCE );
			exactType( LocalTime.class )
					.valueBridge( DefaultLocalTimeBridge.INSTANCE )
					.identifierBridge( DefaultLocalTimeBridge.INSTANCE );
			exactType( ZonedDateTime.class )
					.valueBridge( DefaultZonedDateTimeBridge.INSTANCE )
					.identifierBridge( DefaultZonedDateTimeBridge.INSTANCE );
			exactType( Year.class )
					.valueBridge( DefaultYearBridge.INSTANCE )
					.identifierBridge( DefaultYearBridge.INSTANCE );
			exactType( YearMonth.class )
					.valueBridge( DefaultYearMonthBridge.INSTANCE )
					.identifierBridge( DefaultYearMonthBridge.INSTANCE );
			exactType( MonthDay.class )
					.valueBridge( DefaultMonthDayBridge.INSTANCE )
					.identifierBridge( DefaultMonthDayBridge.INSTANCE );
			exactType( OffsetDateTime.class )
					.valueBridge( DefaultOffsetDateTimeBridge.INSTANCE )
					.identifierBridge( DefaultOffsetDateTimeBridge.INSTANCE );
			exactType( OffsetTime.class )
					.valueBridge( DefaultOffsetTimeBridge.INSTANCE )
					.identifierBridge( DefaultOffsetTimeBridge.INSTANCE );
			exactType( ZoneOffset.class )
					.valueBridge( DefaultZoneOffsetBridge.INSTANCE )
					.identifierBridge( DefaultZoneOffsetBridge.INSTANCE );
			exactType( ZoneId.class )
					.valueBridge( DefaultZoneIdBridge.INSTANCE )
					.identifierBridge( DefaultZoneIdBridge.INSTANCE );
			exactType( Period.class )
					.valueBridge( DefaultPeriodBridge.INSTANCE )
					.identifierBridge( DefaultPeriodBridge.INSTANCE );
			exactType( Duration.class )
					.valueBridge( DefaultDurationBridge.INSTANCE )
					.identifierBridge( DefaultDurationBridge.INSTANCE );

			// java.util
			exactType( UUID.class )
					.valueBridge( DefaultUUIDBridge.INSTANCE )
					.identifierBridge( DefaultUUIDBridge.INSTANCE );
			exactType( Date.class )
					.valueBridge( DefaultJavaUtilDateBridge.INSTANCE )
					.identifierBridge( DefaultJavaUtilDateBridge.INSTANCE );
			exactType( Calendar.class )
					.valueBridge( DefaultJavaUtilCalendarBridge.INSTANCE )
					.identifierBridge( DefaultJavaUtilCalendarBridge.INSTANCE );

			// java.sql
			exactType( java.sql.Date.class )
					.valueBridge( DefaultJavaSqlDateBridge.INSTANCE )
					.identifierBridge( DefaultJavaSqlDateBridge.INSTANCE );
			exactType( Timestamp.class )
					.valueBridge( DefaultJavaSqlTimestampBridge.INSTANCE )
					.identifierBridge( DefaultJavaSqlTimestampBridge.INSTANCE );
			exactType( Time.class )
					.valueBridge( DefaultJavaSqlTimeBridge.INSTANCE )
					.identifierBridge( DefaultJavaSqlTimeBridge.INSTANCE );

			// java.net
			exactType( URI.class )
					.valueBridge( DefaultJavaNetURIBridge.INSTANCE )
					.identifierBridge( DefaultJavaNetURIBridge.INSTANCE );
			exactType( URL.class )
					.valueBridge( DefaultJavaNetURLBridge.INSTANCE )
					.identifierBridge( DefaultJavaNetURLBridge.INSTANCE );

			// org.hibernate.search
			subTypesOf( GeoPoint.class )
					.valueBinder( new StaticValueBinder<>( GeoPoint.class, DefaultGeoPointBridge.INSTANCE ) );
			exactType( GeoPoint.class )
					.identifierBridge( DefaultGeoPointBridge.INSTANCE );
		}

		private class TypePatternDefaultBinderDefinitionStep
				implements DefaultBinderDefinitionStep<TypePatternDefaultBinderDefinitionStep> {
			private final TypePatternMatcher typePatternMatcher;

			private TypePatternDefaultBinderDefinitionStep(TypePatternMatcher typePatternMatcher) {
				this.typePatternMatcher = typePatternMatcher;
			}

			@Override
			public TypePatternDefaultBinderDefinitionStep identifierBinder(IdentifierBinder binder) {
				typePatternIdentifierBridgeMappings.add( new TypePatternBinderMapping<>( typePatternMatcher, binder ) );
				return this;
			}

			@Override
			public TypePatternDefaultBinderDefinitionStep valueBinder(ValueBinder binder) {
				typePatternValueBridgeMappings.add( new TypePatternBinderMapping<>( typePatternMatcher, binder ) );
				return this;
			}
		}

		private class ExactTypeDefaultBridgeDefinitionStep<T>
				implements DefaultBridgeDefinitionStep<ExactTypeDefaultBridgeDefinitionStep<T>, T> {
			private final PojoRawTypeIdentifier<T> typeIdentifier;

			private ExactTypeDefaultBridgeDefinitionStep(PojoRawTypeIdentifier<T> typeIdentifier) {
				this.typeIdentifier = typeIdentifier;
			}

			@Override
			public ExactTypeDefaultBridgeDefinitionStep<T> identifierBinder(IdentifierBinder binder) {
				exactRawTypeIdentifierBridgeMappings.put( typeIdentifier, binder );
				return this;
			}

			@Override
			public ExactTypeDefaultBridgeDefinitionStep<T> valueBinder(ValueBinder binder) {
				exactRawTypeValueBridgeMappings.put( typeIdentifier, binder );
				return this;
			}

			@Override
			public ExactTypeDefaultBridgeDefinitionStep<T> identifierBridge(IdentifierBridge<T> bridge) {
				return identifierBinder( new StaticIdentifierBinder<>( typeIdentifier.javaClass(), bridge ) );
			}

			@Override
			public ExactTypeDefaultBridgeDefinitionStep<T> valueBridge(ValueBridge<T, ?> bridge) {
				return valueBinder( new StaticValueBinder<>( typeIdentifier.javaClass(), bridge ) );
			}
		}
	}

}
