/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.util.common.stub.backend.index.impl;

import java.util.Collections;

import org.hibernate.search.engine.backend.index.spi.StreamIndexWorker;
import org.hibernate.search.engine.common.spi.SessionContext;
import org.hibernate.search.integrationtest.util.common.stub.backend.index.StubIndexWork;
import org.hibernate.search.integrationtest.util.common.stub.backend.document.impl.StubDocumentElement;

class StubStreamIndexWorker extends StubIndexWorker implements StreamIndexWorker<StubDocumentElement> {
	private StubIndexManager indexManager;

	StubStreamIndexWorker(StubIndexManager indexManager, SessionContext context) {
		super( context );
		this.indexManager = indexManager;
	}

	@Override
	public void flush() {
		addWork(
				StubIndexWork.builder( StubIndexWork.Type.FLUSH )
						.tenantIdentifier( sessionContext.getTenantIdentifier() )
						.build()
		);
	}

	@Override
	public void optimize() {
		addWork(
				StubIndexWork.builder( StubIndexWork.Type.OPTIMIZE )
						.tenantIdentifier( sessionContext.getTenantIdentifier() )
						.build()
		);
	}

	@Override
	protected void addWork(StubIndexWork work) {
		indexManager.prepare( Collections.singletonList( work ) );
		indexManager.execute( Collections.singletonList( work ) );
	}
}
