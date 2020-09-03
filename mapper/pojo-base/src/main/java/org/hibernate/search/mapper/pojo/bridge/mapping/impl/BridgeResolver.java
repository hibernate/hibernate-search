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

import org.hibernate.search.engine.cfg.spi.ConvertUtils;
import org.hibernate.search.engine.cfg.spi.ParseUtils;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultBigIntegerIdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultBooleanIdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultByteIdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultCharacterIdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultCharacterValueBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultDoubleIdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultDurationValueBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultEnumIdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultEnumValueBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultFloatIdentifierBridge;
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
import org.hibernate.search.mapper.pojo.bridge.mapping.BridgesConfigurationContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.DefaultBinderDefinitionStep;
import org.hibernate.search.mapper.pojo.bridge.mapping.DefaultBridgeDefinitionStep;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.IdentifierBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.ValueBinder;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.model.spi.PojoBootstrapIntrospector;
import org.hibernate.search.mapper.pojo.model.spi.PojoGenericTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
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

	private static <B> B getBinderOrNull(PojoGenericTypeModel<?> sourceType,
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

		B getBinderIfMatching(PojoGenericTypeModel<?> typeModel) {
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
					.valueBinder( new PassThroughValueBridge.Binder<>( String.class, ParseUtils::parseString ) )
					.identifierBridge( new DefaultStringIdentifierBridge() );
			exactType( Character.class )
					.valueBridge( new DefaultCharacterValueBridge() )
					.identifierBridge( new DefaultCharacterIdentifierBridge() );
			exactType( Boolean.class )
					.valueBinder( new PassThroughValueBridge.Binder<>( Boolean.class, ConvertUtils::convertBoolean ) )
					.identifierBridge( new DefaultBooleanIdentifierBridge() );
			exactType( Byte.class )
					.valueBinder( new PassThroughValueBridge.Binder<>( Byte.class, ConvertUtils::convertByte ) )
					.identifierBridge( new DefaultByteIdentifierBridge() );
			exactType( Short.class )
					.valueBinder( new PassThroughValueBridge.Binder<>( Short.class, ConvertUtils::convertShort ) )
					.identifierBridge( new DefaultShortIdentifierBridge() );
			exactType( Integer.class )
					.valueBinder( new PassThroughValueBridge.Binder<>( Integer.class, ConvertUtils::convertInteger ) )
					.identifierBridge( new DefaultIntegerIdentifierBridge() );
			exactType( Long.class )
					.valueBinder( new PassThroughValueBridge.Binder<>( Long.class, ConvertUtils::convertLong ) )
					.identifierBridge( new DefaultLongIdentifierBridge() );
			exactType( Float.class )
					.valueBinder( new PassThroughValueBridge.Binder<>( Float.class, ConvertUtils::convertFloat ) )
					.identifierBridge( new DefaultFloatIdentifierBridge() );
			exactType( Double.class )
					.valueBinder( new PassThroughValueBridge.Binder<>( Double.class, ConvertUtils::convertDouble ) )
					.identifierBridge( new DefaultDoubleIdentifierBridge() );
			strictSubTypesOf( Enum.class )
					.valueBinder( new DefaultEnumValueBridge.Binder() )
					.identifierBinder( new DefaultEnumIdentifierBridge.Binder() );

			// java.math
			exactType( BigInteger.class )
					.valueBinder( new PassThroughValueBridge.Binder<>( BigInteger.class, ConvertUtils::convertBigInteger ) )
					.identifierBridge( new DefaultBigIntegerIdentifierBridge() );
			exactType( BigDecimal.class )
					.valueBinder( new PassThroughValueBridge.Binder<>( BigDecimal.class, ConvertUtils::convertBigDecimal ) );

			// java.time
			exactType( LocalDate.class )
					.valueBinder( new PassThroughValueBridge.Binder<>( LocalDate.class, ParseUtils::parseLocalDate ) );
			exactType( Instant.class )
					.valueBinder( new PassThroughValueBridge.Binder<>( Instant.class, ParseUtils::parseInstant ) );
			exactType( LocalDateTime.class )
					.valueBinder( new PassThroughValueBridge.Binder<>( LocalDateTime.class, ParseUtils::parseLocalDateTime ) );
			exactType( LocalTime.class )
					.valueBinder( new PassThroughValueBridge.Binder<>( LocalTime.class, ParseUtils::parseLocalTime ) );
			exactType( ZonedDateTime.class )
					.valueBinder( new PassThroughValueBridge.Binder<>( ZonedDateTime.class, ParseUtils::parseZonedDateTime ) );
			exactType( Year.class )
					.valueBinder( new PassThroughValueBridge.Binder<>( Year.class, ParseUtils::parseYear ) );
			exactType( YearMonth.class )
					.valueBinder( new PassThroughValueBridge.Binder<>( YearMonth.class, ParseUtils::parseYearMonth ) );
			exactType( MonthDay.class )
					.valueBinder( new PassThroughValueBridge.Binder<>( MonthDay.class, ParseUtils::parseMonthDay ) );
			exactType( OffsetDateTime.class )
					.valueBinder( new PassThroughValueBridge.Binder<>( OffsetDateTime.class, ParseUtils::parseOffsetDateTime ) );
			exactType( OffsetTime.class )
					.valueBinder( new PassThroughValueBridge.Binder<>( OffsetTime.class, ParseUtils::parseOffsetTime ) );
			exactType( ZoneOffset.class )
					.valueBridge( new DefaultZoneOffsetValueBridge() );
			exactType( ZoneId.class )
					.valueBridge( new DefaultZoneIdValueBridge() );
			exactType( ZoneId.class )
					.valueBridge( new DefaultZoneIdValueBridge() );
			exactType( Period.class )
					.valueBridge( new DefaultPeriodValueBridge() );
			exactType( Duration.class )
					.valueBridge( new DefaultDurationValueBridge() );

			// java.util
			exactType( UUID.class )
					.valueBridge( new DefaultUUIDValueBridge() )
					.identifierBridge( new DefaultUUIDIdentifierBridge() );
			exactType( Date.class )
					.valueBridge( new DefaultJavaUtilDateValueBridge() );
			exactType( Calendar.class )
					.valueBridge( new DefaultJavaUtilCalendarValueBridge() );

			// java.sql
			exactType( java.sql.Date.class )
					.valueBridge( new DefaultJavaSqlDateValueBridge() );
			exactType( Timestamp.class )
					.valueBridge( new DefaultJavaSqlTimestampValueBridge() );
			exactType( Time.class )
					.valueBridge( new DefaultJavaSqlTimeValueBridge() );

			// java.net
			exactType( URI.class )
					.valueBridge( new DefaultJavaNetURIValueBridge() );
			exactType( URL.class )
					.valueBridge( new DefaultJavaNetURLValueBridge() );

			// org.hibernate.search
			subTypesOf( GeoPoint.class )
					.valueBinder( new PassThroughValueBridge.Binder<>( GeoPoint.class, ParseUtils::parseGeoPoint ) );
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
