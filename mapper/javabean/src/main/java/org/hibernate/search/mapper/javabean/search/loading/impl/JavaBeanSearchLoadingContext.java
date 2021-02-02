/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.search.loading.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.engine.search.loading.spi.SearchLoadingContext;
import org.hibernate.search.engine.search.loading.spi.SearchLoadingContextBuilder;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;
import org.hibernate.search.engine.backend.common.spi.DocumentReferenceConverter;
import org.hibernate.search.mapper.javabean.log.impl.Log;
import org.hibernate.search.mapper.javabean.common.EntityReference;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public final class JavaBeanSearchLoadingContext<E> implements SearchLoadingContext<EntityReference, E> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final DocumentReferenceConverter<EntityReference> documentReferenceConverter;

	private JavaBeanSearchLoadingContext(DocumentReferenceConverter<EntityReference> documentReferenceConverter) {
		this.documentReferenceConverter = documentReferenceConverter;
	}

	@Override
	public ProjectionHitMapper<EntityReference, E> createProjectionHitMapper() {
		return new JavaBeanProjectionHitMapper<>( documentReferenceConverter );
	}

	public static final class Builder<E> implements SearchLoadingContextBuilder<EntityReference, E, Void> {
		private final DocumentReferenceConverter<EntityReference> documentReferenceConverter;

		public Builder(DocumentReferenceConverter<EntityReference> documentReferenceConverter) {
			this.documentReferenceConverter = documentReferenceConverter;
		}

		@Override
		public Void toAPI() {
			throw log.entityLoadingConfigurationNotSupported();
		}

		@Override
		public SearchLoadingContext<EntityReference, E> build() {
			return new JavaBeanSearchLoadingContext<>( documentReferenceConverter );
		}
	}
}
