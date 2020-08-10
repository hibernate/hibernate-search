/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.search.loading.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.common.spi.DocumentReferenceConverter;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;
import org.hibernate.search.engine.search.timeout.spi.TimeoutManager;
import org.hibernate.search.mapper.javabean.common.EntityReference;
import org.hibernate.search.mapper.javabean.log.impl.Log;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class JavaBeanProjectionHitMapper implements ProjectionHitMapper<EntityReference, Void> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final DocumentReferenceConverter<EntityReference> documentReferenceConverter;

	public JavaBeanProjectionHitMapper(DocumentReferenceConverter<EntityReference> documentReferenceConverter) {
		this.documentReferenceConverter = documentReferenceConverter;
	}

	@Override
	public Object planLoading(DocumentReference reference) {
		throw log.cannotLoadEntity( reference );
	}

	@Override
	public LoadingResult<EntityReference, Void> loadBlocking(TimeoutManager timeoutManager) {
		return new JavaBeanUnusuableGetLoadingResult( documentReferenceConverter );
	}

	private static class JavaBeanUnusuableGetLoadingResult implements LoadingResult<EntityReference, Void> {

		private final DocumentReferenceConverter<EntityReference> documentReferenceConverter;

		private JavaBeanUnusuableGetLoadingResult(DocumentReferenceConverter<EntityReference> documentReferenceConverter) {
			this.documentReferenceConverter = documentReferenceConverter;
		}

		@Override
		public Void get(Object key) {
			throw new AssertionFailure(
					"Attempt to load an entity with a key that was never issued."
							+ " There is probably a bug in Hibernate Search, please report it."
			);
		}

		@Override
		public EntityReference convertReference(DocumentReference reference) {
			return documentReferenceConverter.fromDocumentReference( reference );
		}

	}
}
