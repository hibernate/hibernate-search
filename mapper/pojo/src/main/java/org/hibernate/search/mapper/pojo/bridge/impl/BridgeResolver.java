/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.impl;

import java.lang.invoke.MethodHandles;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultEnumIdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultEnumValueBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultIntegerIdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultJavaUtilDateValueBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultLongIdentifierBridge;
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

		addIdentifierBridgeForExactRawType( Integer.class, ignored -> new DefaultIntegerIdentifierBridge() );
		addIdentifierBridgeForExactRawType( Long.class, ignored -> new DefaultLongIdentifierBridge() );
		addIdentifierBridgeForTypePattern( concreteEnumPattern, ignored -> new DefaultEnumIdentifierBridge<>() );

		addValueBridgeForExactRawType( Integer.class, ignored -> new PassThroughValueBridge<>( Integer.class ) );
		addValueBridgeForExactRawType( Long.class, ignored -> new PassThroughValueBridge<>( Long.class ) );
		addValueBridgeForExactRawType( Boolean.class, ignored -> new PassThroughValueBridge<>( Boolean.class ) );
		addValueBridgeForExactRawType( String.class, ignored -> new PassThroughValueBridge<>( String.class ) );
		addValueBridgeForExactRawType( LocalDate.class, ignored -> new PassThroughValueBridge<>( LocalDate.class ) );
		addValueBridgeForExactRawType( Instant.class, ignored -> new PassThroughValueBridge<>( Instant.class ) );
		addValueBridgeForExactRawType( Date.class, ignored -> new DefaultJavaUtilDateValueBridge() );
		addValueBridgeForTypePattern( concreteEnumPattern, ignored -> new DefaultEnumValueBridge<>() );
	}

	public BridgeBuilder<? extends IdentifierBridge<?>> resolveIdentifierBridgeForType(PojoGenericTypeModel<?> sourceType) {
		@SuppressWarnings("unchecked")
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
		@SuppressWarnings("unchecked")
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
		@SuppressWarnings("unchecked")
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
