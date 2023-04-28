/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.search.definition.binding.builtin;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.definition.ProjectionDefinitionContext;
import org.hibernate.search.engine.search.projection.definition.spi.AbstractProjectionDefinition;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.mapper.pojo.search.definition.binding.ProjectionBinder;
import org.hibernate.search.mapper.pojo.search.definition.binding.ProjectionBindingContext;

/**
 * Binds a constructor parameter to a projection to a
 * {@link org.hibernate.search.engine.backend.common.DocumentReference} representing the hit.
 *
 * @see SearchProjectionFactory#documentReference()
 * @see org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentReferenceProjection
 */
public final class DocumentReferenceProjectionBinder implements ProjectionBinder {
	private static final DocumentReferenceProjectionBinder INSTANCE = new DocumentReferenceProjectionBinder();

	/**
	 * Creates a {@link DocumentReferenceProjectionBinder} to be passed
	 * to {@link org.hibernate.search.mapper.pojo.mapping.definition.programmatic.MethodParameterMappingStep#projection(ProjectionBinder)}.
	 *
	 * @return The binder.
	 */
	public static DocumentReferenceProjectionBinder create() {
		return INSTANCE;
	}

	private DocumentReferenceProjectionBinder() {
	}

	@Override
	public void bind(ProjectionBindingContext context) {
		context.definition( DocumentReference.class, Definition.INSTANCE );
	}

	private static class Definition extends AbstractProjectionDefinition<DocumentReference> {
		public static final Definition INSTANCE = new Definition();

		@Override
		protected String type() {
			return "document-reference";
		}

		@Override
		public SearchProjection<DocumentReference> create(SearchProjectionFactory<?, ?> factory,
				ProjectionDefinitionContext context) {
			return factory.documentReference().toProjection();
		}
	}

}
