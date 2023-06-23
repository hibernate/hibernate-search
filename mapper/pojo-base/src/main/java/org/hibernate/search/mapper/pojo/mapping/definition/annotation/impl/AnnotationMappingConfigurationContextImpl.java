/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation.impl;

import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.environment.classpath.spi.ClassResolver;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingBuildContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingConfigurationCollector;
import org.hibernate.search.engine.mapper.model.spi.MappableTypeModel;
import org.hibernate.search.engine.mapper.model.spi.TypeMetadataDiscoverer;
import org.hibernate.search.engine.reporting.spi.FailureCollector;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoTypeMetadataContributor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AnnotationMappingConfigurationContext;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.spi.BuiltinAnnotations;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingConfigurationContext;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingConfigurationContributor;
import org.hibernate.search.mapper.pojo.model.spi.PojoBootstrapIntrospector;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.util.common.jar.impl.JandexUtils;
import org.hibernate.search.util.common.jar.impl.JarUtils;
import org.hibernate.search.util.common.jar.spi.JandexBehavior;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reflect.spi.AnnotationHelper;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.IndexView;

public class AnnotationMappingConfigurationContextImpl
		implements AnnotationMappingConfigurationContext,
		PojoMappingConfigurationContributor {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final PojoBootstrapIntrospector introspector;

	private boolean discoverAnnotatedTypesFromRootMappingAnnotations = false;
	private boolean discoverJandexIndexesFromAddedTypes = false;
	private boolean buildMissingJandexIndexes = false;
	private boolean discoverAnnotationsFromReferencedTypes = false;

	// Use a LinkedHashSet for deterministic iteration
	private final Set<Class<?>> explicitAnnotatedTypes = new LinkedHashSet<>();
	private final List<IndexView> explicitJandexIndexes = new ArrayList<>();

	public AnnotationMappingConfigurationContextImpl(PojoBootstrapIntrospector introspector) {
		this.introspector = introspector;
	}

	@Override
	public AnnotationMappingConfigurationContext discoverAnnotatedTypesFromRootMappingAnnotations(boolean enabled) {
		this.discoverAnnotatedTypesFromRootMappingAnnotations = enabled;
		return this;
	}

	@Override
	public AnnotationMappingConfigurationContext discoverJandexIndexesFromAddedTypes(boolean enabled) {
		this.discoverJandexIndexesFromAddedTypes = enabled;
		return this;
	}

	@Override
	public AnnotationMappingConfigurationContext buildMissingDiscoveredJandexIndexes(boolean enabled) {
		this.buildMissingJandexIndexes = enabled;
		return this;
	}

	@Override
	public AnnotationMappingConfigurationContext discoverAnnotationsFromReferencedTypes(boolean enabled) {
		this.discoverAnnotationsFromReferencedTypes = enabled;
		return this;
	}

	@Override
	public AnnotationMappingConfigurationContext add(Class<?> annotatedType) {
		this.explicitAnnotatedTypes.add( annotatedType );
		return this;
	}

	@Override
	public AnnotationMappingConfigurationContext add(Set<Class<?>> annotatedTypes) {
		this.explicitAnnotatedTypes.addAll( annotatedTypes );
		return this;
	}

	@Override
	public AnnotationMappingConfigurationContext addJandexIndex(IndexView jandexIndex) {
		explicitJandexIndexes.add( jandexIndex );
		return this;
	}

	@Override
	public void configure(MappingBuildContext buildContext, PojoMappingConfigurationContext configurationContext,
			MappingConfigurationCollector<PojoTypeMetadataContributor> collector) {
		BeanResolver beanResolver = buildContext.beanResolver();
		FailureCollector failureCollector = buildContext.failureCollector();
		AnnotationHelper annotationHelper = new AnnotationHelper( introspector.annotationValueHandleFactory() );
		AnnotationPojoTypeMetadataContributorFactory contributorFactory =
				new AnnotationPojoTypeMetadataContributorFactory( beanResolver, failureCollector, configurationContext,
						annotationHelper );

		Set<PojoRawTypeModel<?>> typesToProcess = new LinkedHashSet<>();

		for ( Class<?> annotatedType : explicitAnnotatedTypes ) {
			introspector.typeModel( annotatedType ).ascendingSuperTypes()
					.forEach( typesToProcess::add );
		}

		if ( discoverAnnotatedTypesFromRootMappingAnnotations ) {
			JandexBehavior.doWithJandex( () -> {
				IndexView jandexIndex = buildJandexIndex();
				if ( jandexIndex == null ) {
					return;
				}
				discoverAnnotatedTypesFromRootMappingAnnotation( typesToProcess, jandexIndex,
						buildContext.classResolver() );
			} );
		}

		Set<PojoRawTypeModel<?>> alreadyContributedTypes = new HashSet<>();
		for ( PojoRawTypeModel<?> typeModel : typesToProcess ) {
			boolean neverContributed = alreadyContributedTypes.add( typeModel );
			// Ignore types that were already contributed
			// TODO optimize by completely ignoring standard Java types, e.g. Object or standard Java interfaces such as Serializable?
			if ( neverContributed ) {
				Optional<PojoTypeMetadataContributor> contributorOptional =
						contributorFactory.createIfAnnotated( typeModel );
				if ( contributorOptional.isPresent() ) {
					collector.collectContributor( typeModel, contributorOptional.get() );
				}
			}
		}

		/*
		 * If automatic discovery of annotations on referenced types is enabled,
		 * also add a discoverer for new types (e.g. types encountered in an @IndexedEmbedded).
		 */
		if ( discoverAnnotationsFromReferencedTypes ) {
			PojoAnnotationTypeMetadataDiscoverer discoverer =
					new PojoAnnotationTypeMetadataDiscoverer( contributorFactory, alreadyContributedTypes );
			collector.collectDiscoverer( discoverer );
		}
	}

	private void discoverAnnotatedTypesFromRootMappingAnnotation(Set<PojoRawTypeModel<?>> annotatedTypes,
			IndexView jandexIndex, ClassResolver classResolver) {
		Set<DotName> rootMappingAnnotations = new HashSet<>( BuiltinAnnotations.ROOT_MAPPING_ANNOTATIONS );
		rootMappingAnnotations.addAll(
				JandexUtils.findAnnotatedAnnotationsAndContaining( jandexIndex, BuiltinAnnotations.ROOT_MAPPING ) );

		Set<DotName> rootMappingAnnotatedTypes = new HashSet<>();
		for ( DotName annotationName : rootMappingAnnotations ) {
			for ( AnnotationInstance annotation : jandexIndex.getAnnotations( annotationName ) ) {
				ClassInfo annotatedClassInfo = JandexUtils.extractDeclaringClass( annotation.target() );
				rootMappingAnnotatedTypes.add( annotatedClassInfo.name() );
			}
		}

		for ( DotName rootMappingAnnotatedType : rootMappingAnnotatedTypes ) {
			Class<?> annotatedClass = classResolver.classForName( rootMappingAnnotatedType.toString() );
			introspector.typeModel( annotatedClass ).ascendingSuperTypes()
					.forEach( annotatedTypes::add );
		}
	}

	private IndexView buildJandexIndex() {
		List<IndexView> jandexIndexes = new ArrayList<>( explicitJandexIndexes );

		if ( discoverJandexIndexesFromAddedTypes ) {
			IndexView compositeOfExplicitJandexIndexes = JandexUtils.compositeIndex( jandexIndexes );
			Set<URL> discoveredBuildingAllowedCodeSourceLocations = new LinkedHashSet<>();
			Set<URL> discoveredBuildingForbiddenCodeSourceLocations = new LinkedHashSet<>();
			for ( Class<?> annotatedType : explicitAnnotatedTypes ) {
				DotName dotName = DotName.createSimple( annotatedType.getName() );
				// Optimization: if a class is already in the Jandex index,
				// there's no need to discover the Jandex index of its JAR.
				if ( compositeOfExplicitJandexIndexes.getClassByName( dotName ) == null ) {
					Set<URL> targetSet = isJandexBuildingAllowed( annotatedType )
							? discoveredBuildingAllowedCodeSourceLocations
							: discoveredBuildingForbiddenCodeSourceLocations;
					JarUtils.codeSourceLocation( annotatedType ).ifPresent( targetSet::add );
				}
			}
			for ( URL codeSourceLocation : discoveredBuildingAllowedCodeSourceLocations ) {
				jandexIndexForCodeSourceLocation( codeSourceLocation, true ).ifPresent( jandexIndexes::add );
			}
			for ( URL codeSourceLocation : discoveredBuildingForbiddenCodeSourceLocations ) {
				jandexIndexForCodeSourceLocation( codeSourceLocation, false ).ifPresent( jandexIndexes::add );
			}
		}

		return jandexIndexes.isEmpty() ? null : JandexUtils.compositeIndex( jandexIndexes );
	}

	private boolean isJandexBuildingAllowed(Class<?> annotatedType) {
		if ( buildMissingJandexIndexes ) {
			Package pakkage = annotatedType.getPackage();
			// We expect Hibernate projects to always provide a Jandex index if one is needed.
			return pakkage != null
					&& !pakkage.getName().equals( "org.hibernate" )
					&& !pakkage.getName().startsWith( "org.hibernate." );
		}
		else {
			return false;
		}
	}

	private static Optional<Index> jandexIndexForCodeSourceLocation(URL codeSourceLocation, boolean buildIfMissing) {
		try {
			if ( buildIfMissing ) {
				return Optional.of( JandexUtils.readOrBuildIndex( codeSourceLocation ) );
			}
			else {
				return JandexUtils.readIndex( codeSourceLocation );
			}
		}
		catch (RuntimeException e) {
			throw log.errorDiscoveringJandexIndex( codeSourceLocation, e.getMessage(), e );
		}
	}

	/**
	 * A type metadata discoverer that will provide annotation-based metadata
	 * for types that were not explicitly requested .
	 */
	private static class PojoAnnotationTypeMetadataDiscoverer implements TypeMetadataDiscoverer<PojoTypeMetadataContributor> {
		private final AnnotationPojoTypeMetadataContributorFactory contributorFactory;
		private final Set<PojoRawTypeModel<?>> alreadyContributedTypes;

		PojoAnnotationTypeMetadataDiscoverer(AnnotationPojoTypeMetadataContributorFactory contributorFactory,
				Set<PojoRawTypeModel<?>> alreadyContributedTypes) {
			this.contributorFactory = contributorFactory;
			this.alreadyContributedTypes = alreadyContributedTypes;
		}

		@Override
		public Optional<PojoTypeMetadataContributor> discover(MappableTypeModel typeModel) {
			PojoRawTypeModel<?> pojoTypeModel = (PojoRawTypeModel<?>) typeModel;
			/*
			 * Take care of not adding duplicate contributors: this could lead to mapping errors,
			 * for instance a field being declared twice.
			 */
			boolean neverContributed = alreadyContributedTypes.add( pojoTypeModel );
			if ( neverContributed ) {
				// TODO optimize by completely ignoring standard Java types, e.g. Object or standard Java interfaces such as Serializable?
				return contributorFactory.createIfAnnotated( pojoTypeModel );
			}
			else {
				return Optional.empty();
			}
		}
	}
}
