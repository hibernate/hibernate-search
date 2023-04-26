/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.mapper.stub;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.common.EntityReference;
import org.hibernate.search.engine.common.timing.Deadline;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;
import org.hibernate.search.util.common.AssertionFailure;

final class StubProjectionHitMapper implements ProjectionHitMapper<DocumentReference> {

	private final List<DocumentReference> referencesToLoad = new ArrayList<>();

	@Override
	public Object planLoading(DocumentReference reference) {
		referencesToLoad.add( reference );
		return referencesToLoad.size() - 1;
	}

	@Override
	public LoadingResult<DocumentReference> loadBlocking(Deadline deadline) {
		return new StubLoadingResult( referencesToLoad );
	}

	private static class StubLoadingResult implements LoadingResult<DocumentReference> {

		private final List<DocumentReference> referencesToLoad;

		private StubLoadingResult(List<DocumentReference> referencesToLoad) {
			this.referencesToLoad = referencesToLoad;
		}

		@Override
		public DocumentReference get(Object key) {
			return referencesToLoad.get( (int) key );
		}

		@Override
		public EntityReference convertReference(DocumentReference reference) {
			throw new AssertionFailure( "Entity references cannot be retrieved with the default loading context."
					+ " Use StubMappedIndex#createGenericScope(...) to test loading/reference-related features" );
		}
	}
}
