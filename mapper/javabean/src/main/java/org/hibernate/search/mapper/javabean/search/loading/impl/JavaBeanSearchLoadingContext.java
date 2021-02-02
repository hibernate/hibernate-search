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

public final class JavaBeanSearchLoadingContext implements SearchLoadingContext<EntityReference, Void> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final DocumentReferenceConverter<EntityReference> documentReferenceConverter;

	private JavaBeanSearchLoadingContext(DocumentReferenceConverter<EntityReference> documentReferenceConverter) {
		this.documentReferenceConverter = documentReferenceConverter;
	}

	@Override
	public ProjectionHitMapper<EntityReference, Void> createProjectionHitMapper() {
		return new JavaBeanProjectionHitMapper( documentReferenceConverter );
	}

	public static final class Builder implements SearchLoadingContextBuilder<EntityReference, Void, Void> {
		private final DocumentReferenceConverter<EntityReference> documentReferenceConverter;

		public Builder(DocumentReferenceConverter<EntityReference> documentReferenceConverter) {
			this.documentReferenceConverter = documentReferenceConverter;
		}

		@Override
		public Void toAPI() {
			throw log.entityLoadingConfigurationNotSupported();
		}

		@Override
		public SearchLoadingContext<EntityReference, Void> build() {
			return new JavaBeanSearchLoadingContext( documentReferenceConverter );
		}
	}
}
