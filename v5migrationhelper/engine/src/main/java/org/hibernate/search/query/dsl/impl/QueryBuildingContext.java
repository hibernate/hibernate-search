/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.query.dsl.impl;

import java.util.Map;
import java.util.Optional;

import org.hibernate.search.engine.backend.metamodel.IndexFieldDescriptor;
import org.hibernate.search.engine.backend.metamodel.IndexValueFieldDescriptor;
import org.hibernate.search.engine.backend.metamodel.IndexValueFieldTypeDescriptor;
import org.hibernate.search.scope.spi.V5MigrationSearchScope;
import org.hibernate.search.spi.SearchIntegrator;

/**
 * Keep the query builder contextual information
 *
 * @author Emmanuel Bernard
 */
public class QueryBuildingContext {

	private final SearchIntegrator integrator;
	private final V5MigrationSearchScope scope;
	private final Map<String, String> analyzerOverrides;

	public QueryBuildingContext(SearchIntegrator integrator, V5MigrationSearchScope scope,
			Map<String, String> analyzerOverrides) {
		this.integrator = integrator;
		this.scope = scope;
		this.analyzerOverrides = analyzerOverrides;
	}

	public SearchIntegrator getIntegrator() {
		return integrator;
	}

	public V5MigrationSearchScope getScope() {
		return scope;
	}

	public String getOriginalAnalyzer(String fieldName) {
		return scope.indexManagers().stream()
				.map( im -> im.descriptor().field( fieldName ).filter( IndexFieldDescriptor::isValueField )
						.map( IndexFieldDescriptor::toValueField )
						.map( IndexValueFieldDescriptor::type )
						.flatMap( IndexValueFieldTypeDescriptor::analyzerName ) )
				.filter( Optional::isPresent )
				.map( Optional::get )
				.findFirst()
				.orElse( null );
	}

	public String getOverriddenAnalyzer(String fieldName) {
		return analyzerOverrides.get( fieldName );
	}
}
