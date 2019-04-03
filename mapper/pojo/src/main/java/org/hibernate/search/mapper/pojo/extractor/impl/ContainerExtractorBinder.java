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

import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanProvider;
import org.hibernate.search.engine.mapper.mapping.spi.MappingBuildContext;
import org.hibernate.search.mapper.pojo.extractor.ContainerExtractor;
import org.hibernate.search.mapper.pojo.extractor.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.extractor.builtin.impl.ArrayElementExtractor;
import org.hibernate.search.mapper.pojo.extractor.builtin.impl.CollectionElementExtractor;
import org.hibernate.search.mapper.pojo.extractor.builtin.impl.IterableElementExtractor;
import org.hibernate.search.mapper.pojo.extractor.builtin.impl.MapValueExtractor;
import org.hibernate.search.mapper.pojo.extractor.builtin.impl.OptionalDoubleValueExtractor;
import org.hibernate.search.mapper.pojo.extractor.builtin.impl.OptionalIntValueExtractor;
import org.hibernate.search.mapper.pojo.extractor.builtin.impl.OptionalLongValueExtractor;
import org.hibernate.search.mapper.pojo.extractor.builtin.impl.OptionalValueExtractor;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.model.spi.PojoGenericTypeModel;
import org.hibernate.search.mapper.pojo.model.typepattern.impl.ExtractingTypePatternMatcher;
import org.hibernate.search.mapper.pojo.model.typepattern.impl.TypePatternMatcherFactory;
import org.hibernate.search.mapper.pojo.util.impl.GenericTypeContext;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.impl.SuppressingCloser;

/**
 * Binds {@link ContainerExtractorPath}s to a given input type,
 * and allows to create extractors for a given {@link BoundContainerExtractorPath}.
 * <p>
 * The {@link ContainerExtractorPath} is independent from the input type.
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
 * This "binding" results in a {@link BoundContainerExtractorPath},
 * which carries both a {@link ContainerExtractorPath}
 * (which is an explicit list of classes, and never {@link ContainerExtractorPath#defaultExtractors()},
 * since the default path was resolved) and the resulting value type.
 * <p>
 * From this "bound path", the {@link ContainerExtractorBinder} is able to later create
 * a {@link ContainerExtractor}, which can be used at runtime to extract values from a container.
 */
public class ContainerExtractorBinder {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	// TODO add an extension point to override the builtin extractors, or at least to add defaults for other types

	private final BeanProvider beanProvider;
	private final TypePatternMatcherFactory typePatternMatcherFactory;
	private final FirstMatchingExtractorContributor firstMatchingExtractorContributor =
			new FirstMatchingExtractorContributor();
	@SuppressWarnings("rawtypes") // Checks are implemented using reflection
	private final Map<Class<? extends ContainerExtractor>, ExtractorContributor> extractorContributorCache =
			new HashMap<>();

	public ContainerExtractorBinder(MappingBuildContext buildContext,
			TypePatternMatcherFactory typePatternMatcherFactory) {
		this.beanProvider = buildContext.getBeanProvider();
		this.typePatternMatcherFactory = typePatternMatcherFactory;
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
	 * i.e. to resolve the possibly implicit extractor path ({@link ContainerExtractorPath#defaultExtractors()})
	 * and to validate that all extractors in the path can be applied.
	 *
	 * @param sourceType A model of the source type to apply extractors to.
	 * @param extractorPath The list of extractors to apply.
	 * @param <C> The source type.
	 * @return The resolved extractor path, or an empty optional if
	 * one of the extractors in the path cannot be applied.
	 */
	@SuppressWarnings({"rawtypes"}) // Checks are implemented using reflection
	public <C> Optional<BoundContainerExtractorPath<C, ?>> tryBindPath(PojoGenericTypeModel<C> sourceType,
			ContainerExtractorPath extractorPath) {
		ExtractorResolutionState<C> state = new ExtractorResolutionState<>( sourceType );
		if ( extractorPath.isDefault() ) {
			firstMatchingExtractorContributor.tryAppend( state );
		}
		else {
			for ( Class<? extends ContainerExtractor> extractorClass
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
	 * i.e. resolve the possibly implicit extractor path ({@link ContainerExtractorPath#defaultExtractors()})
	 * and validate that all extractors in the path can be applied,
	 * or fail.
	 *
	 * @param sourceType A model of the source type to apply extractors to.
	 * @param extractorPath The list of extractors to apply.
	 * @param <C> The source type.
	 * @return The bound extractor path.
	 * @throws SearchException if
	 * one of the extractors in the path cannot be applied.
	 */
	@SuppressWarnings({"rawtypes"}) // Checks are implemented using reflection
	public <C> BoundContainerExtractorPath<C, ?> bindPath(PojoGenericTypeModel<C> sourceType,
			ContainerExtractorPath extractorPath) {
		ExtractorResolutionState<C> state = new ExtractorResolutionState<>( sourceType );
		if ( extractorPath.isDefault() ) {
			firstMatchingExtractorContributor.tryAppend( state );
		}
		else {
			for ( Class<? extends ContainerExtractor> extractorClass
					: extractorPath.getExplicitExtractorClasses() ) {
				ExtractorContributor extractorContributor = getExtractorContributorForClass( extractorClass );
				if ( !extractorContributor.tryAppend( state ) ) {
					throw log.invalidContainerExtractorForType( extractorClass, state.extractedType );
				}
			}
		}
		return state.build();
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
	// Checks are performed using reflection when building the resolved path
	@SuppressWarnings( {"rawtypes", "unchecked"} )
	public <C, V> ContainerExtractorHolder<C, V> create(BoundContainerExtractorPath<C, V> boundPath) {
		if ( boundPath.getExtractorPath().isEmpty() ) {
			throw new AssertionFailure(
					"Received a request to create extractors, but the extractor path was empty."
							+ " There is probably a bug in Hibernate Search."
			);
		}
		ContainerExtractor<? super C, ?> extractor = null;
		List<BeanHolder<?>> beanHolders = new ArrayList<>();
		try {
			for ( Class<? extends ContainerExtractor> extractorClass :
					boundPath.getExtractorPath().getExplicitExtractorClasses() ) {
				BeanHolder<? extends ContainerExtractor> newExtractorHolder =
						beanProvider.getBean( extractorClass );
				beanHolders.add( newExtractorHolder );
				if ( extractor == null ) {
					// First extractor: must be able to process type C
					extractor = (ContainerExtractor<? super C, ?>) newExtractorHolder.get();
				}
				else {
					extractor = new ChainingContainerExtractor( extractor, newExtractorHolder.get() );
				}
			}
			return new ContainerExtractorHolder<>(
					(ContainerExtractor<? super C, V>) extractor, beanHolders
			);
		}
		catch (RuntimeException e) {
			new SuppressingCloser( e ).pushAll( BeanHolder::close, beanHolders );
			throw e;
		}
	}

	public boolean isDefaultExtractorPath(PojoGenericTypeModel<?> sourceType, ContainerExtractorPath extractorPath) {
		Optional<? extends BoundContainerExtractorPath<?, ?>> boundDefaultExtractorPathOptional =
				tryBindPath(
						sourceType,
						ContainerExtractorPath.defaultExtractors()
				);
		return boundDefaultExtractorPathOptional.isPresent() && extractorPath.equals(
				boundDefaultExtractorPathOptional.get().getExtractorPath()
		);
	}

	@SuppressWarnings( "rawtypes" ) // Checks are implemented using reflection
	private void addDefaultExtractor(Class<? extends ContainerExtractor> extractorClass) {
		ExtractorContributor extractorContributor = getExtractorContributorForClass( extractorClass );
		firstMatchingExtractorContributor.addCandidate( extractorContributor );
	}

	@SuppressWarnings( "rawtypes" ) // Checks are implemented using reflection
	private ExtractorContributor getExtractorContributorForClass(
			Class<? extends ContainerExtractor> extractorClass) {
		return extractorContributorCache.computeIfAbsent( extractorClass, this::createExtractorContributorForClass );
	}

	@SuppressWarnings( "rawtypes" ) // Checks are implemented using reflection
	private ExtractorContributor createExtractorContributorForClass(
			Class<? extends ContainerExtractor> extractorClass) {
		GenericTypeContext typeContext = new GenericTypeContext( extractorClass );
		Type typePattern = typeContext.resolveTypeArgument( ContainerExtractor.class, 0 )
				.orElseThrow( () -> log.cannotInferContainerExtractorClassTypePattern( extractorClass ) );
		Type typeToExtract = typeContext.resolveTypeArgument( ContainerExtractor.class, 1 )
				.orElseThrow( () -> log.cannotInferContainerExtractorClassTypePattern( extractorClass ) );
		ExtractingTypePatternMatcher typePatternMatcher;
		try {
			typePatternMatcher = typePatternMatcherFactory.createExtractingMatcher( typePattern, typeToExtract );
		}
		catch (UnsupportedOperationException e) {
			throw log.cannotInferContainerExtractorClassTypePattern( extractorClass );
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
		private final ExtractingTypePatternMatcher typePatternMatcher;
		private final Class<? extends ContainerExtractor> extractorClass;

		SingleExtractorContributor(ExtractingTypePatternMatcher typePatternMatcher,
				Class<? extends ContainerExtractor> extractorClass) {
			this.typePatternMatcher = typePatternMatcher;
			this.extractorClass = extractorClass;
		}

		@Override
		public boolean tryAppend(ExtractorResolutionState<?> state) {
			Optional<? extends PojoGenericTypeModel<?>> resultTypeOptional =
					typePatternMatcher.extract( state.extractedType );
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

	@SuppressWarnings({"rawtypes"}) // Checks are implemented using reflection
	private static class ExtractorResolutionState<C> {

		private final List<Class<? extends ContainerExtractor>> extractorClasses = new ArrayList<>();
		private final PojoGenericTypeModel<C> sourceType;
		private PojoGenericTypeModel<?> extractedType;

		ExtractorResolutionState(PojoGenericTypeModel<C> sourceType) {
			this.sourceType = sourceType;
			this.extractedType = sourceType;
		}

		void append(Class<? extends ContainerExtractor> extractorClass, PojoGenericTypeModel<?> extractedType) {
			extractorClasses.add( extractorClass );
			this.extractedType = extractedType;
		}

		BoundContainerExtractorPath<C, ?> build() {
			return new BoundContainerExtractorPath<>(
					sourceType,
					ContainerExtractorPath.explicitExtractors( extractorClasses ),
					extractedType
			);
		}

	}
}
