/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.building.impl;

import java.lang.invoke.MethodHandles;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.hibernate.search.engine.common.tree.TreeFilterDefinition;
import org.hibernate.search.engine.common.tree.spi.TreeFilterPathTracker;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexedEntityBindingMapperContext;
import org.hibernate.search.engine.mapper.model.spi.MappingElement;
import org.hibernate.search.engine.mapper.model.spi.TypeMetadataContributorProvider;
import org.hibernate.search.engine.reporting.spi.ContextualFailureCollector;
import org.hibernate.search.engine.reporting.spi.FailureCollector;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoTypeMetadataContributor;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.building.impl.PojoTypeAdditionalMetadataProvider;
import org.hibernate.search.mapper.pojo.model.spi.PojoBootstrapIntrospector;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class PojoMappingHelper implements IndexedEntityBindingMapperContext {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final BeanResolver beanResolver;
	private final ContextualFailureCollector failureCollector;
	private final TypeMetadataContributorProvider<PojoTypeMetadataContributor> contributorProvider;
	private final PojoBootstrapIntrospector introspector;
	private final PojoTypeAdditionalMetadataProvider typeAdditionalMetadataProvider;
	private final PojoIndexModelBinder indexModelBinder;

	// Use a LinkedHashMap for deterministic iteration
	private final Map<MappingElement, TreeFilterPathTracker> pathTrackers = new LinkedHashMap<>();

	PojoMappingHelper(BeanResolver beanResolver,
			ContextualFailureCollector failureCollector,
			TypeMetadataContributorProvider<PojoTypeMetadataContributor> contributorProvider,
			PojoBootstrapIntrospector introspector,
			PojoTypeAdditionalMetadataProvider typeAdditionalMetadataProvider,
			PojoIndexModelBinder indexModelBinder) {
		this.beanResolver = beanResolver;
		this.failureCollector = failureCollector;
		this.contributorProvider = contributorProvider;
		this.introspector = introspector;
		this.typeAdditionalMetadataProvider = typeAdditionalMetadataProvider;
		this.indexModelBinder = indexModelBinder;
	}

	public BeanResolver beanResolver() {
		return beanResolver;
	}

	public FailureCollector failureCollector() {
		return failureCollector;
	}

	public TypeMetadataContributorProvider<PojoTypeMetadataContributor> contributorProvider() {
		return contributorProvider;
	}

	public PojoBootstrapIntrospector introspector() {
		return introspector;
	}

	public PojoIndexModelBinder indexModelBinder() {
		return indexModelBinder;
	}

	public PojoTypeAdditionalMetadataProvider typeAdditionalMetadataProvider() {
		return typeAdditionalMetadataProvider;
	}

	@Override
	public TreeFilterPathTracker getOrCreatePathTracker(MappingElement mappingElement,
			TreeFilterDefinition filterDefinition) {
		TreeFilterPathTracker result = pathTrackers.get( mappingElement );
		if ( result != null ) {
			return result;
		}
		result = new TreeFilterPathTracker( filterDefinition );
		pathTrackers.put( mappingElement, result );
		return result;
	}

	void checkPathTrackers() {
		for ( Map.Entry<MappingElement, TreeFilterPathTracker> entry : pathTrackers.entrySet() ) {
			TreeFilterPathTracker pathTracker = entry.getValue();
			Set<String> uselessIncludePaths = pathTracker.uselessIncludePaths();
			if ( !uselessIncludePaths.isEmpty() ) {
				Set<String> encounteredFieldPaths = pathTracker.encounteredFieldPaths();
				failureCollector.add( log.uselessIncludePathFilters(
						entry.getKey(),
						uselessIncludePaths, encounteredFieldPaths,
						entry.getKey().eventContext()
				) );
			}

			Set<String> uselessExcludePaths = pathTracker.uselessExcludePaths();
			// this would mean that we have a path in excludes that is unavailable
			if ( !uselessExcludePaths.isEmpty() ) {
				Set<String> encounteredFieldPaths = pathTracker.encounteredFieldPaths();
				failureCollector.add( log.uselessExcludePathFilters(
						entry.getKey(),
						uselessExcludePaths, encounteredFieldPaths,
						entry.getKey().eventContext()
				) );
			}
		}
	}

}
