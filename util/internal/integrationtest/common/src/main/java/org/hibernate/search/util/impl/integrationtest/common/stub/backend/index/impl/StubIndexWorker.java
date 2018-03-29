/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.impl;

import org.hibernate.search.engine.backend.index.spi.DocumentContributor;
import org.hibernate.search.engine.backend.index.spi.DocumentReferenceProvider;
import org.hibernate.search.engine.backend.index.spi.IndexWorker;
import org.hibernate.search.engine.common.spi.SessionContext;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.StubIndexWork;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.impl.StubDocumentElement;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.StubDocumentNode;

abstract class StubIndexWorker implements IndexWorker<StubDocumentElement> {

	final SessionContext sessionContext;

	StubIndexWorker(SessionContext sessionContext) {
		this.sessionContext = sessionContext;
	}

	@Override
	public void add(DocumentReferenceProvider documentReferenceProvider,
			DocumentContributor<StubDocumentElement> documentContributor) {
		StubIndexWork.Builder builder = StubIndexWork.builder( StubIndexWork.Type.ADD );
		populate( builder, documentReferenceProvider );
		StubDocumentNode.Builder documentBuilder = StubDocumentNode.document();
		StubDocumentElement documentElement = new StubDocumentElement( documentBuilder );
		documentContributor.contribute( documentElement );
		builder.document( documentBuilder.build() );
		addWork( builder.build() );
	}

	@Override
	public void update(DocumentReferenceProvider documentReferenceProvider,
			DocumentContributor<StubDocumentElement> documentContributor) {
		StubIndexWork.Builder builder = StubIndexWork.builder( StubIndexWork.Type.UPDATE );
		populate( builder, documentReferenceProvider );
		StubDocumentNode.Builder documentBuilder = StubDocumentNode.document();
		StubDocumentElement documentElement = new StubDocumentElement( documentBuilder );
		documentContributor.contribute( documentElement );
		builder.document( documentBuilder.build() );
		addWork( builder.build() );
	}

	@Override
	public void delete(DocumentReferenceProvider documentReferenceProvider) {
		StubIndexWork.Builder builder = StubIndexWork.builder( StubIndexWork.Type.DELETE );
		populate( builder, documentReferenceProvider );
		addWork( builder.build() );
	}

	protected final void populate(StubIndexWork.Builder builder, DocumentReferenceProvider documentReferenceProvider) {
		builder.tenantIdentifier( sessionContext.getTenantIdentifier() );
		builder.identifier( documentReferenceProvider.getIdentifier() );
		builder.routingKey( documentReferenceProvider.getRoutingKey() );
	}

	protected abstract void addWork(StubIndexWork work);
}
