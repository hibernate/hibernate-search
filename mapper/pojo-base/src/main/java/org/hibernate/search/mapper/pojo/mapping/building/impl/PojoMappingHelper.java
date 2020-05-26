/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.building.impl;

import org.hibernate.search.engine.mapper.mapping.building.spi.TypeMetadataContributorProvider;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoTypeMetadataContributor;
import org.hibernate.search.engine.reporting.spi.FailureCollector;
import org.hibernate.search.mapper.pojo.model.spi.PojoBootstrapIntrospector;

public class PojoMappingHelper {

	private final FailureCollector failureCollector;
	private final TypeMetadataContributorProvider<PojoTypeMetadataContributor> contributorProvider;
	private final PojoBootstrapIntrospector introspector;
	private final PojoIndexModelBinder indexModelBinder;

	PojoMappingHelper(FailureCollector failureCollector,
			TypeMetadataContributorProvider<PojoTypeMetadataContributor> contributorProvider,
			PojoBootstrapIntrospector introspector,
			PojoIndexModelBinder indexModelBinder) {
		this.failureCollector = failureCollector;
		this.contributorProvider = contributorProvider;
		this.introspector = introspector;
		this.indexModelBinder = indexModelBinder;
	}

	public FailureCollector getFailureCollector() {
		return failureCollector;
	}

	public TypeMetadataContributorProvider<PojoTypeMetadataContributor> getContributorProvider() {
		return contributorProvider;
	}

	public PojoBootstrapIntrospector getIntrospector() {
		return introspector;
	}

	public PojoIndexModelBinder getIndexModelBinder() {
		return indexModelBinder;
	}

}
