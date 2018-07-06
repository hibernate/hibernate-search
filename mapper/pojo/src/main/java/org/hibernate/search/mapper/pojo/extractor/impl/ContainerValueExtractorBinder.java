/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.extractor.impl;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.hibernate.search.engine.common.spi.BeanProvider;
import org.hibernate.search.engine.mapper.mapping.spi.MappingBuildContext;
import org.hibernate.search.mapper.pojo.extractor.ContainerValueExtractor;
import org.hibernate.search.mapper.pojo.extractor.ContainerValueExtractorPath;
import org.hibernate.search.mapper.pojo.extractor.builtin.ArrayElementExtractor;
import org.hibernate.search.mapper.pojo.extractor.builtin.CollectionElementExtractor;
import org.hibernate.search.mapper.pojo.extractor.builtin.IterableElementExtractor;
import org.hibernate.search.mapper.pojo.extractor.builtin.MapValueExtractor;
import org.hibernate.search.mapper.pojo.extractor.builtin.OptionalDoubleValueExtractor;
import org.hibernate.search.mapper.pojo.extractor.builtin.OptionalIntValueExtractor;
import org.hibernate.search.mapper.pojo.extractor.builtin.OptionalLongValueExtractor;
import org.hibernate.search.mapper.pojo.extractor.builtin.OptionalValueExtractor;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.model.spi.PojoBootstrapIntrospector;
import org.hibernate.search.mapper.pojo.model.spi.PojoGenericTypeModel;
import org.hibernate.search.mapper.pojo.model.typepattern.impl.TypePatternMatcher;
import org.hibernate.search.mapper.pojo.model.typepattern.impl.TypePatternMatcherFactory;
import org.hibernate.search.mapper.pojo.util.impl.GenericTypeContext;
import org.hibernate.search.util.AssertionFailure;
import org.hibernate.search.util.impl.common.LoggerFactory;

/**
 * Binds {@link ContainerValueExtractorPath}s to a given input type,
 * and allows to create extractors for a given {@link BoundContainerValueExtractorPath}.
 * <p>
 * The {@link ContainerValueExtractorPath} is independent from the input type.
 * This means in particular that the path needs to "bound" to an input type before it can be useful:
 * <ul>
 *     <li>First to check that the path applies correctly: {@link CollectionElementExtractor}
 *     won't work on a {@link Map}.
 *     <li>Second to determine the resulting value type:
 *     {@code [MapValueExtractor.class, CollectionElementExtractor.class]} applied
 *     to a {@code Map<String, Collection<Integer>>} will result in {@code Integer} values.
 *     <li>Third, in the case of the default path, to determine the exact list of extractor classes.
 *     For instance, for a {@code Collection<String>} the default path will be resolved
 *     to {@link CollectionElementExtractor}.
 *     For a {@code Map<String, Collection<Integer>>} the default path will be resolved
 *     to {@code [MapValueExtractor.class, CollectionElementExtractor.class]}.
 * </ul>
 * This "binding" results in a {@link BoundContainerValueExtractorPath},
 * which carries both a {@link ContainerValueExtractorPath}
 * (which is an explicit list of classes, and never {@link ContainerValueExtractorPath#defaultExtractors()},
 * since the default path was resolved) and the resulting value type.
 * <p>
 * From this "bound path", the {@link ContainerValueExtractorBinder} is able to later create
 * a {@link ContainerValueExtractor}, which can be used at runtime to extract values from a container.
 */
public class ContainerValueExtractorBinder {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	// TODO add an extension point to override the builtin extractors, or at least to add defaults for other types

	private final BeanProvider beanProvider;
	private final TypePatternMatcherFactory typePatternMatcherFactory = new TypePatternMatcherFactory();
	private final FirstMatchingExtractorContributor firstMatchingExtractorContributor =
			new FirstMatchingExtractorContributor();
	@SuppressWarnings("rawtypes") // Checks are implemented using reflection
	private Map<Class<? extends ContainerValueExtractor>, ExtractorContributor> extractorContributorCache =
			new HashMap<>();

	public ContainerValueExtractorBinder(MappingBuildContext buildContext) {
		this.beanProvider = buildContext.getServiceManager().getBeanProvider();
		addDefaultExtractor( MapValueExtractor.class );
		addDefaultExtractor( CollectionElementExtractor.class );
		addDefaultExtractor( IterableElementExtractor.class );
		addDefaultExtractor( OptionalValueExtractor.class );
		addDefaultExtractor( OptionalIntValueExtractor.class );
		addDefaultExtractor( OptionalLongValueExtractor.class );
		addDefaultExtractor( OptionalDoubleValueExtractor.class );
		addDefaultExtractor( ArrayElementExtractor.class );
	}

	/**
	 * Try to bind a container extractor path to a given source type,
	 * i.e. to resolve the possibly implicit extractor path ({@link ContainerValueExtractorPath#defaultExtractors()})
	 * and to validate that all extractors in the path can be applied.
	 *
	 * @param introspector An introspector, to retrieve type models.
	 * @param sourceType A model of the source type to apply extractors to.
	 * @param extractorPath The list of extractors to apply.
	 * @param <C> The source type.
	 * @return The resolved extractor path, or an empty optional if
	 * one of the extractors in the path cannot be applied.
	 */
	@SuppressWarnings("unchecked") // Checks are implemented using reflection
	public <C> Optional<BoundContainerValueExtractorPath<C, ?>> tryBindPath(
			PojoBootstrapIntrospector introspector, PojoGenericTypeModel<C> sourceType,
			ContainerValueExtractorPath extractorPath) {
		ExtractorResolutionState<C> state = new ExtractorResolutionState<>( introspector, sourceType );
		if ( extractorPath.isDefault() ) {
			firstMatchingExtractorContributor.tryAppend( state );
		}
		else {
			for ( Class<? extends ContainerValueExtractor> extractorClass
					: extractorPath.getExplicitExtractorClasses() ) {
				ExtractorContributor extractorContributor = getExtractorContributorForClass( extractorClass );
				if ( !extractorContributor.tryAppend( state ) ) {
					/*
					 * Assume failure, even if a previous extractor was applied successfully:
					 * we want either every extractor to be applied, or none.
					 */
					return Optional.empty();
				}
			}
		}
		return Optional.of( state.build() );
	}

	/**
	 * Bind a container extractor path to a given source type,
	 * i.e. resolve the possibly implicit extractor path ({@link ContainerValueExtractorPath#defaultExtractors()})
	 * and validate that all extractors in the path can be applied,
	 * or fail.
	 *
	 * @param introspector An introspector, to retrieve type models.
	 * @param sourceType A model of the source type to apply extractors to.
	 * @param extractorPath The list of extractors to apply.
	 * @param <C> The source type.
	 * @return The bound extractor path.
	 * @throws org.hibernate.search.util.SearchException if
	 * one of the extractors in the path cannot be applied.
	 */
	@SuppressWarnings("unchecked") // Checks are implemented using reflection
	public <C> BoundContainerValueExtractorPath<C, ?> bindPath(
			PojoBootstrapIntrospector introspector, PojoGenericTypeModel<C> sourceType,
			ContainerValueExtractorPath extractorPath) {
		ExtractorResolutionState<C> state = new ExtractorResolutionState<>( introspector, sourceType );
		if ( extractorPath.isDefault() ) {
			firstMatchingExtractorContributor.tryAppend( state );
		}
		else {
			for ( Class<? extends ContainerValueExtractor> extractorClass
					: extractorPath.getExplicitExtractorClasses() ) {
				ExtractorContributor extractorContributor = getExtractorContributorForClass( extractorClass );
				if ( !extractorContributor.tryAppend( state ) ) {
					throw log.invalidContainerValueExtractorForType( extractorClass, state.extractedType );
				}
			}
		}
		return state.build();
	}

	/**
	 * Attempts to create a container value extractor from a bound path.
	 *
	 * @param boundPath The bound path to create the extractor from.
	 * @param <C> The source type.
	 * @param <V> The extracted value type.
	 * @return The extractor, or an empty optional if the bound path was empty.
	 */
	// Checks are performed using reflection when building the resolved path
	@SuppressWarnings( {"rawtypes", "unchecked"} )
	public <C, V> Optional<ContainerValueExtractor<? super C, V>> tryCreate(
			BoundContainerValueExtractorPath<C, V> boundPath) {
		ContainerValueExtractor<? super C, ?> extractor = null;
		for ( Class<? extends ContainerValueExtractor> extractorClass :
				boundPath.getExtractorPath().getExplicitExtractorClasses() ) {
			ContainerValueExtractor<?, ?> newExtractor =
					beanProvider.getBean( extractorClass, ContainerValueExtractor.class );
			if ( extractor == null ) {
				// First extractor: must be able to process type C
				extractor = (ContainerValueExtractor<? super C, ?>) newExtractor;
			}
			else {
				extractor = new ChainingContainerValueExtractor( extractor, newExtractor );
			}
		}
		if ( extractor == null ) {
			return Optional.empty();
		}
		else {
			return Optional.of( (ContainerValueExtractor<C, V>) extractor );
		}
	}

	/**
	 * Create a container value extractor from a bound path, or fail.
	 *
	 * @param boundPath The bound path to create the extractor from.
	 * @param <C> The source type.
	 * @param <V> The extracted value type.
	 * @return The extractor.
	 * @throws AssertionFailure if the bound path was empty
	 */
	@SuppressWarnings("unchecked") // Checks are implemented using reflection
	public <C, V> ContainerValueExtractor<? super C, V> create(BoundContainerValueExtractorPath<C, V> boundPath) {
		if ( boundPath.getExtractorPath().isEmpty() ) {
			throw new AssertionFailure(
					"Received a request to create extractors, but the extractor path was empty."
					+ " There is probably a bug in Hibernate Search."
			);
		}
		// tryCreate will always return a non-empty result in this case, since the resolved path is non-empty
		return tryCreate( boundPath ).get();
	}

	public boolean isDefaultExtractorPath(PojoBootstrapIntrospector introspector, PojoGenericTypeModel<?> sourceType,
			ContainerValueExtractorPath extractorPath) {
		Optional<? extends BoundContainerValueExtractorPath<?, ?>> boundDefaultExtractorPathOptional =
				tryBindPath(
						introspector, sourceType,
						ContainerValueExtractorPath.defaultExtractors()
				);
		return boundDefaultExtractorPathOptional.isPresent() && extractorPath.equals(
				boundDefaultExtractorPathOptional.get().getExtractorPath()
		);
	}

	@SuppressWarnings( "rawtypes" ) // Checks are implemented using reflection
	private void addDefaultExtractor(Class<? extends ContainerValueExtractor> extractorClass) {
		ExtractorContributor extractorContributor = getExtractorContributorForClass( extractorClass );
		firstMatchingExtractorContributor.addCandidate( extractorContributor );
	}

	@SuppressWarnings( "rawtypes" ) // Checks are implemented using reflection
	private ExtractorContributor getExtractorContributorForClass(
			Class<? extends ContainerValueExtractor> extractorClass) {
		return extractorContributorCache.computeIfAbsent( extractorClass, this::createExtractorContributorForClass );
	}

	@SuppressWarnings( "rawtypes" ) // Checks are implemented using reflection
	private ExtractorContributor createExtractorContributorForClass(
			Class<? extends ContainerValueExtractor> extractorClass) {
		GenericTypeContext typeContext = new GenericTypeContext( extractorClass );
		Type typeToMatch = typeContext.resolveTypeArgument( ContainerValueExtractor.class, 0 )
				.orElseThrow( () -> log.cannotInferContainerValueExtractorClassTypePattern( extractorClass ) );
		Type resultType = typeContext.resolveTypeArgument( ContainerValueExtractor.class, 1 )
				.orElseThrow( () -> log.cannotInferContainerValueExtractorClassTypePattern( extractorClass ) );
		TypePatternMatcher typePatternMatcher;
		try {
			typePatternMatcher = typePatternMatcherFactory.create( typeToMatch, resultType );
		}
		catch (UnsupportedOperationException e) {
			throw log.cannotInferContainerValueExtractorClassTypePattern( extractorClass );
		}
		return new SingleExtractorContributor( typePatternMatcher, extractorClass );
	}

	private interface ExtractorContributor {

		/**
		 * @param state The state to append an extractor to
		 * @return {@code true} if the current type was accepted by this contributor and an extractor was added,
		 * {@code false} if the type was rejected and no extractor was added.
		 */
		boolean tryAppend(ExtractorResolutionState<?> state);

	}

	@SuppressWarnings( "rawtypes" ) // Checks are implemented using reflection
	private class SingleExtractorContributor implements ExtractorContributor {
		private final TypePatternMatcher typePatternMatcher;
		private final Class<? extends ContainerValueExtractor> extractorClass;

		SingleExtractorContributor(TypePatternMatcher typePatternMatcher,
				Class<? extends ContainerValueExtractor> extractorClass) {
			this.typePatternMatcher = typePatternMatcher;
			this.extractorClass = extractorClass;
		}

		@Override
		public boolean tryAppend(ExtractorResolutionState<?> state) {
			Optional<? extends PojoGenericTypeModel<?>> resultTypeOptional =
					typePatternMatcher.match( state.introspector, state.extractedType );
			if ( resultTypeOptional.isPresent() ) {
				state.append( extractorClass, resultTypeOptional.get() );
				return true;
			}
			else {
				return false;
			}
		}
	}

	private static class FirstMatchingExtractorContributor implements ExtractorContributor {
		private final List<ExtractorContributor> candidates = new ArrayList<>();

		void addCandidate(ExtractorContributor contributor) {
			candidates.add( contributor );
		}

		@Override
		public boolean tryAppend(ExtractorResolutionState<?> state) {
			for ( ExtractorContributor extractorContributor : candidates ) {
				if ( extractorContributor.tryAppend( state ) ) {
					// Recurse as much as possible
					tryAppend( state );
					return true;
				}
			}
			return false;
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" }) // Checks are implemented using reflection
	private static class ExtractorResolutionState<C> {

		private final PojoBootstrapIntrospector introspector;
		private final List<Class<? extends ContainerValueExtractor>> extractorClasses = new ArrayList<>();
		private final PojoGenericTypeModel<C> sourceType;
		private PojoGenericTypeModel<?> extractedType;

		ExtractorResolutionState(PojoBootstrapIntrospector introspector, PojoGenericTypeModel<C> sourceType) {
			this.introspector = introspector;
			this.sourceType = sourceType;
			this.extractedType = sourceType;
		}

		void append(Class<? extends ContainerValueExtractor> extractorClass, PojoGenericTypeModel<?> extractedType) {
			extractorClasses.add( extractorClass );
			this.extractedType = extractedType;
		}

		BoundContainerValueExtractorPath<C, ?> build() {
			return new BoundContainerValueExtractorPath<>(
					sourceType,
					ContainerValueExtractorPath.explicitExtractors( extractorClasses ),
					extractedType
			);
		}

	}
}
