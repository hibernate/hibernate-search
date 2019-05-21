/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.extractor.spi;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.search.mapper.pojo.extractor.ContainerExtractor;
import org.hibernate.search.mapper.pojo.extractor.ContainerExtractorDefinitionContext;
import org.hibernate.search.mapper.pojo.extractor.builtin.BuiltinContainerExtractors;
import org.hibernate.search.mapper.pojo.extractor.builtin.impl.ArrayElementExtractor;
import org.hibernate.search.mapper.pojo.extractor.builtin.impl.CollectionElementExtractor;
import org.hibernate.search.mapper.pojo.extractor.builtin.impl.IterableElementExtractor;
import org.hibernate.search.mapper.pojo.extractor.builtin.impl.MapKeyExtractor;
import org.hibernate.search.mapper.pojo.extractor.builtin.impl.MapValueExtractor;
import org.hibernate.search.mapper.pojo.extractor.builtin.impl.OptionalDoubleValueExtractor;
import org.hibernate.search.mapper.pojo.extractor.builtin.impl.OptionalIntValueExtractor;
import org.hibernate.search.mapper.pojo.extractor.builtin.impl.OptionalLongValueExtractor;
import org.hibernate.search.mapper.pojo.extractor.builtin.impl.OptionalValueExtractor;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

@SuppressWarnings("rawtypes") // We need to allow raw container types, e.g. MapValueExtractor.class
public final class ContainerExtractorRegistry {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	public static Builder builder() {
		return new Builder();
	}

	private final Map<String, Class<? extends ContainerExtractor>> extractorsByName = new HashMap<>();
	private final List<Class<? extends ContainerExtractor>> defaultExtractors = new ArrayList<>();

	private ContainerExtractorRegistry(Map<String, Class<? extends ContainerExtractor>> customExtractorsByName) {
		extractorsByName.putAll( customExtractorsByName );

		// Caution: the order of calls below is meaningful
		addDefaultExtractor( BuiltinContainerExtractors.MAP_VALUE, MapValueExtractor.class );
		addDefaultExtractor( BuiltinContainerExtractors.COLLECTION, CollectionElementExtractor.class );
		addDefaultExtractor( BuiltinContainerExtractors.ITERABLE, IterableElementExtractor.class );
		addDefaultExtractor( BuiltinContainerExtractors.OPTIONAL, OptionalValueExtractor.class );
		addDefaultExtractor( BuiltinContainerExtractors.OPTIONAL_INT, OptionalIntValueExtractor.class );
		addDefaultExtractor( BuiltinContainerExtractors.OPTIONAL_LONG, OptionalLongValueExtractor.class );
		addDefaultExtractor( BuiltinContainerExtractors.OPTIONAL_DOUBLE, OptionalDoubleValueExtractor.class );
		addDefaultExtractor( BuiltinContainerExtractors.ARRAY, ArrayElementExtractor.class );

		addNonDefaultExtractor( BuiltinContainerExtractors.MAP_KEY, MapKeyExtractor.class );
	}

	public List<Class<? extends ContainerExtractor>> getDefaults() {
		return Collections.unmodifiableList( defaultExtractors );
	}

	public Class<? extends ContainerExtractor> getForName(String name) {
		Class<? extends ContainerExtractor> result = extractorsByName.get( name );
		if ( result == null ) {
			throw log.cannotResolveContainerExtractorName( name, BuiltinContainerExtractors.class );
		}
		return result;
	}

	private void addDefaultExtractor(String name, Class<? extends ContainerExtractor> extractorClass) {
		extractorsByName.put( name, extractorClass );
		defaultExtractors.add( extractorClass );
	}

	private void addNonDefaultExtractor(String name, Class<? extends ContainerExtractor> extractorClass) {
		extractorsByName.put( name, extractorClass );
	}

	public static final class Builder implements ContainerExtractorDefinitionContext {
		private final Map<String, Class<? extends ContainerExtractor>> extractorsByName = new HashMap<>();

		private Builder() {
		}

		@Override
		public void define(String extractorName, Class<? extends ContainerExtractor> extractorClass) {
			extractorsByName.put( extractorName, extractorClass );
		}

		public ContainerExtractorRegistry build() {
			return new ContainerExtractorRegistry( extractorsByName );
		}
	}
}
