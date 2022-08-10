/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.mapper.stub;

import org.hibernate.search.engine.search.projection.definition.spi.CompositeProjectionDefinition;
import org.hibernate.search.engine.search.projection.definition.spi.ProjectionRegistry;
import org.hibernate.search.util.common.AssertionFailure;

public final class StubMappingFixture {

	private final StubMappingImpl mapping;

	ProjectionRegistry projectionRegistry = new ProjectionRegistry() {
		@Override
		public <T> CompositeProjectionDefinition<T> composite(Class<T> objectClass) {
			throw new AssertionFailure( "Projection definitions are not supported in the stub mapper,"
					+ " unless a projection registry is set explicitly through StubMapping#withProjectionRegistry" );
		}
	};

	StubMappingFixture(StubMappingImpl mapping) {
		this.mapping = mapping;
	}

	public StubMappingFixture projectionRegistry(ProjectionRegistry registry) {
		this.projectionRegistry = registry;
		return this;
	}

	public void run(Runnable runnable) {
		StubMappingFixture oldFixture = mapping.fixture;
		mapping.fixture = this;
		try {
			runnable.run();
		}
		finally {
			mapping.fixture = oldFixture;
		}
	}
}
