/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.extractor.spi;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.BeanRetrieval;
import org.hibernate.search.mapper.pojo.extractor.ContainerExtractor;
import org.hibernate.search.mapper.pojo.extractor.ContainerExtractorConfigurationContext;
import org.hibernate.search.mapper.pojo.extractor.builtin.BuiltinContainerExtractors;
import org.hibernate.search.mapper.pojo.extractor.builtin.impl.BooleanArrayElementExtractor;
import org.hibernate.search.mapper.pojo.extractor.builtin.impl.ByteArrayElementExtractor;
import org.hibernate.search.mapper.pojo.extractor.builtin.impl.CharArrayElementExtractor;
import org.hibernate.search.mapper.pojo.extractor.builtin.impl.CollectionElementExtractor;
import org.hibernate.search.mapper.pojo.extractor.builtin.impl.DoubleArrayElementExtractor;
import org.hibernate.search.mapper.pojo.extractor.builtin.impl.FloatArrayElementExtractor;
import org.hibernate.search.mapper.pojo.extractor.builtin.impl.IntArrayElementExtractor;
import org.hibernate.search.mapper.pojo.extractor.builtin.impl.IterableElementExtractor;
import org.hibernate.search.mapper.pojo.extractor.builtin.impl.LongArrayElementExtractor;
import org.hibernate.search.mapper.pojo.extractor.builtin.impl.MapKeyExtractor;
import org.hibernate.search.mapper.pojo.extractor.builtin.impl.MapValueExtractor;
import org.hibernate.search.mapper.pojo.extractor.builtin.impl.ObjectArrayElementExtractor;
import org.hibernate.search.mapper.pojo.extractor.builtin.impl.OptionalDoubleValueExtractor;
import org.hibernate.search.mapper.pojo.extractor.builtin.impl.OptionalIntValueExtractor;
import org.hibernate.search.mapper.pojo.extractor.builtin.impl.OptionalLongValueExtractor;
import org.hibernate.search.mapper.pojo.extractor.builtin.impl.OptionalValueExtractor;
import org.hibernate.search.mapper.pojo.extractor.builtin.impl.ShortArrayElementExtractor;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

@SuppressWarnings("rawtypes") // We need to allow raw container types, e.g. MapValueExtractor.class
public final class ContainerExtractorRegistry {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	public static Builder builder() {
		return new Builder();
	}

	private final Map<String, ContainerExtractorDefinition<? extends ContainerExtractor>> extractorsByName = new HashMap<>();
	private final List<String> defaultExtractors = new ArrayList<>();

	private ContainerExtractorRegistry(
			Map<String, ContainerExtractorDefinition<? extends ContainerExtractor>> customExtractorsByName) {
		extractorsByName.putAll( customExtractorsByName );

		// Caution: the order of calls below is meaningful
		addDefaultExtractor( BuiltinContainerExtractors.MAP_VALUE, MapValueExtractor.class );
		addDefaultExtractor( BuiltinContainerExtractors.COLLECTION, CollectionElementExtractor.class );
		addDefaultExtractor( BuiltinContainerExtractors.ITERABLE, IterableElementExtractor.class );
		addDefaultExtractor( BuiltinContainerExtractors.OPTIONAL, OptionalValueExtractor.class );
		addDefaultExtractor( BuiltinContainerExtractors.OPTIONAL_INT, OptionalIntValueExtractor.class );
		addDefaultExtractor( BuiltinContainerExtractors.OPTIONAL_LONG, OptionalLongValueExtractor.class );
		addDefaultExtractor( BuiltinContainerExtractors.OPTIONAL_DOUBLE, OptionalDoubleValueExtractor.class );
		addDefaultExtractor( BuiltinContainerExtractors.ARRAY_CHAR, CharArrayElementExtractor.class );
		addDefaultExtractor( BuiltinContainerExtractors.ARRAY_BOOLEAN, BooleanArrayElementExtractor.class );
		addDefaultExtractor( BuiltinContainerExtractors.ARRAY_BYTE, ByteArrayElementExtractor.class );
		addDefaultExtractor( BuiltinContainerExtractors.ARRAY_SHORT, ShortArrayElementExtractor.class );
		addDefaultExtractor( BuiltinContainerExtractors.ARRAY_INT, IntArrayElementExtractor.class );
		addDefaultExtractor( BuiltinContainerExtractors.ARRAY_LONG, LongArrayElementExtractor.class );
		addDefaultExtractor( BuiltinContainerExtractors.ARRAY_FLOAT, FloatArrayElementExtractor.class );
		addDefaultExtractor( BuiltinContainerExtractors.ARRAY_DOUBLE, DoubleArrayElementExtractor.class );
		addDefaultExtractor( BuiltinContainerExtractors.ARRAY_OBJECT, ObjectArrayElementExtractor.class );

		addNonDefaultExtractor( BuiltinContainerExtractors.MAP_KEY, MapKeyExtractor.class );
	}

	public List<String> defaults() {
		return Collections.unmodifiableList( defaultExtractors );
	}

	public ContainerExtractorDefinition<?> forName(String name) {
		ContainerExtractorDefinition<?> result = extractorsByName.get( name );
		if ( result == null ) {
			throw log.cannotResolveContainerExtractorName( name, BuiltinContainerExtractors.class );
		}
		return result;
	}

	private <C extends ContainerExtractor> void addDefaultExtractor(String name, Class<C> extractorClass) {
		addNonDefaultExtractor( name, extractorClass );
		defaultExtractors.add( name );
	}

	private <C extends ContainerExtractor> void addNonDefaultExtractor(String name, Class<C> extractorClass) {
		extractorsByName.put( name, new ContainerExtractorDefinition<>( extractorClass,
				BeanReference.of( extractorClass, BeanRetrieval.CONSTRUCTOR ) ) );
	}

	public static final class Builder implements ContainerExtractorConfigurationContext {
		private final Map<String, ContainerExtractorDefinition<?>> extractorsByName = new HashMap<>();

		private Builder() {
		}

		@Override
		public void define(String extractorName, Class<? extends ContainerExtractor> extractorClass) {
			doDefine( extractorName, extractorClass );
		}

		private <C extends ContainerExtractor> void doDefine(String extractorName, Class<C> extractorClass) {
			define( extractorName, extractorClass, BeanReference.of( extractorClass, BeanRetrieval.CONSTRUCTOR ) );
		}

		@Override
		public <C extends ContainerExtractor> void define(String extractorName, Class<C> extractorClass,
				BeanReference<? extends C> reference) {
			extractorsByName.put( extractorName, new ContainerExtractorDefinition<>( extractorClass, reference ) );
		}

		public ContainerExtractorRegistry build() {
			return new ContainerExtractorRegistry( extractorsByName );
		}
	}
}
