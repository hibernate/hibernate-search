/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.mapper.stub;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.search.projection.definition.spi.CompositeProjectionDefinition;
import org.hibernate.search.engine.search.projection.definition.spi.ProjectionRegistry;
import org.hibernate.search.engine.search.projection.spi.ProjectionMappedTypeContext;
import org.hibernate.search.util.common.AssertionFailure;

public final class StubMappingFixture {

	private static final CompositeProjectionDefinition<DocumentReference> DEFAULT_DOC_REF_PROJECTION =
			(f, initialStep) -> initialStep.from( f.documentReference() ).as( Function.identity() );
	private static final ProjectionRegistry DEFAULT_PROJECTION_REGISTRY = new ProjectionRegistry() {
		@Override
		public <T> CompositeProjectionDefinition<T> composite(Class<T> objectClass) {
			Optional<CompositeProjectionDefinition<T>> optional = compositeOptional( objectClass );
			return optional.orElseThrow(
					() -> new AssertionFailure( "Projection definitions are not supported in the stub mapper,"
							+ " unless a projection registry is set explicitly through StubMapping#withProjectionRegistry" ) );
		}

		@Override
		@SuppressWarnings("unchecked")
		public <T> Optional<CompositeProjectionDefinition<T>> compositeOptional(Class<T> objectClass) {
			if ( DocumentReference.class.equals( objectClass ) ) {
				return Optional.of( (CompositeProjectionDefinition<T>) DEFAULT_DOC_REF_PROJECTION );
			}
			return Optional.empty();
		}
	};
	private final StubMappingImpl mapping;

	Map<String, ProjectionMappedTypeContext> typeContexts;

	ProjectionRegistry projectionRegistry = DEFAULT_PROJECTION_REGISTRY;

	StubMappingFixture(StubMappingImpl mapping) {
		this.mapping = mapping;
	}

	public StubMappingFixture typeContext(String name, ProjectionMappedTypeContext typeContext) {
		if ( this.typeContexts == null ) {
			this.typeContexts = new HashMap<>();
		}
		this.typeContexts.put( name, typeContext );
		return this;
	}

	ProjectionMappedTypeContext typeContext(String name) {
		if ( this.typeContexts != null ) {
			ProjectionMappedTypeContext context = typeContexts.get( name );
			if ( context == null ) {
				throw new AssertionFailure( "Missing custom type context for name '" + name
						+ "' in stub mapping fixture (mapping.with().[...])" );
			}
			return context;
		}
		else {
			// Default, legacy behavior: many tests rely on that.
			return new ProjectionMappedTypeContext() {
				@Override
				public String name() {
					return name;
				}

				@Override
				public Class<?> javaClass() {
					return DocumentReference.class;
				}

				@Override
				public boolean loadingAvailable() {
					return false;
				}
			};
		}
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
