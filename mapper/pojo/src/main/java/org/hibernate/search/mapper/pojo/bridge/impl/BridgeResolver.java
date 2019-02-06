/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.impl;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultBigIntegerIdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultEnumIdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultEnumValueBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultIntegerIdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultJavaUtilDateValueBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultLongIdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultShortIdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultUUIDIdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.PassThroughValueBridge;
import org.hibernate.search.mapper.pojo.bridge.mapping.BridgeBuilder;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.model.spi.PojoGenericTypeModel;
import org.hibernate.search.mapper.pojo.model.typepattern.impl.TypePatternMatcher;
import org.hibernate.search.mapper.pojo.model.typepattern.impl.TypePatternMatcherFactory;
import org.hibernate.search.util.impl.common.LoggerFactory;

/**
 * @author Yoann Rodiere
 */
public final class BridgeResolver {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final Map<Class<?>, BridgeBuilder<? extends IdentifierBridge<?>>> exactRawTypeIdentifierBridgeMappings = new HashMap<>();
	private final Map<Class<?>, BridgeBuilder<? extends ValueBridge<?, ?>>> exactRawTypeValueBridgeMappings = new HashMap<>();

	private final List<TypePatternBridgeMapping<? extends IdentifierBridge<?>>> typePatternIdentifierBridgeMappings = new ArrayList<>();
	private final List<TypePatternBridgeMapping<? extends ValueBridge<?, ?>>> typePatternValueBridgeMappings = new ArrayList<>();

	public BridgeResolver(TypePatternMatcherFactory typePatternMatcherFactory) {
		// TODO add an extension point to override these maps, or at least to add defaults for other types
		// TODO add defaults for other types (byte, char, Characeter, Double, double, boolean, Boolean, ...)

		TypePatternMatcher concreteEnumPattern = typePatternMatcherFactory.createRawSuperTypeMatcher( Enum.class )
				.and( typePatternMatcherFactory.createExactRawTypeMatcher( Enum.class ).negate() );

		addIdentifierBridgeForExactRawType( Integer.class, ignored -> BeanHolder.of( new DefaultIntegerIdentifierBridge() ) );
		addIdentifierBridgeForExactRawType( Long.class, ignored -> BeanHolder.of( new DefaultLongIdentifierBridge() ) );
		addIdentifierBridgeForTypePattern( concreteEnumPattern, ignored -> BeanHolder.of( new DefaultEnumIdentifierBridge<>() ) );
		addIdentifierBridgeForExactRawType( Short.class, ignored -> BeanHolder.of( new DefaultShortIdentifierBridge() ) );
		addIdentifierBridgeForExactRawType( BigInteger.class, ignored -> BeanHolder.of( new DefaultBigIntegerIdentifierBridge() ) );
		addIdentifierBridgeForExactRawType( UUID.class, ignored -> BeanHolder.of( new DefaultUUIDIdentifierBridge() ) );

		addValueBridgeForExactRawType( Integer.class, ignored -> BeanHolder.of( new PassThroughValueBridge<>( Integer.class ) ) );
		addValueBridgeForExactRawType( Long.class, ignored -> BeanHolder.of( new PassThroughValueBridge<>( Long.class ) ) );
		addValueBridgeForExactRawType( Boolean.class, ignored -> BeanHolder.of( new PassThroughValueBridge<>( Boolean.class ) ) );
		addValueBridgeForExactRawType( String.class, ignored -> BeanHolder.of( new PassThroughValueBridge<>( String.class ) ) );
		addValueBridgeForExactRawType( LocalDate.class, ignored -> BeanHolder.of( new PassThroughValueBridge<>( LocalDate.class ) ) );
		addValueBridgeForExactRawType( Instant.class, ignored -> BeanHolder.of( new PassThroughValueBridge<>( Instant.class ) ) );
		addValueBridgeForExactRawType( Date.class, ignored -> BeanHolder.of( new DefaultJavaUtilDateValueBridge() ) );
		addValueBridgeForTypePattern( concreteEnumPattern, ignored -> BeanHolder.of( new DefaultEnumValueBridge<>() ) );
		addValueBridgeForExactRawType( Character.class, ignored -> BeanHolder.of( new PassThroughValueBridge<>( Character.class ) ) );
		addValueBridgeForExactRawType( Byte.class, ignored -> BeanHolder.of( new PassThroughValueBridge<>( Byte.class ) ) );
		addValueBridgeForExactRawType( Short.class, ignored -> BeanHolder.of( new PassThroughValueBridge<>( Short.class ) ) );
		addValueBridgeForExactRawType( Float.class, ignored -> BeanHolder.of( new PassThroughValueBridge<>( Float.class ) ) );
		addValueBridgeForExactRawType( Double.class, ignored -> BeanHolder.of( new PassThroughValueBridge<>( Double.class ) ) );
		addValueBridgeForExactRawType( BigDecimal.class, ignored -> BeanHolder.of( new PassThroughValueBridge<>( BigDecimal.class ) ) );
		addValueBridgeForExactRawType( BigInteger.class, ignored -> BeanHolder.of( new PassThroughValueBridge<>( BigInteger.class ) ) );
		addValueBridgeForExactRawType( UUID.class, ignored -> BeanHolder.of( new PassThroughValueBridge<>( UUID.class ) ) );
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
