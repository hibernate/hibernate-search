/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.util.common.stub.backend.index.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.index.spi.ChangesetIndexWorker;
import org.hibernate.search.engine.common.spi.SessionContext;
import org.hibernate.search.integrationtest.util.common.stub.backend.index.StubIndexWork;
import org.hibernate.search.integrationtest.util.common.stub.backend.document.impl.StubDocumentElement;

class StubChangesetIndexWorker extends StubIndexWorker
		implements ChangesetIndexWorker<StubDocumentElement> {
	private final StubIndexManager indexManager;

	private final List<StubIndexWork> works = new ArrayList<>();

	private int preparedIndex = 0;

	StubChangesetIndexWorker(StubIndexManager indexManager, SessionContext context) {
		super( context );
		this.indexManager = indexManager;
	}

	@Override
	protected void addWork(StubIndexWork work) {
		works.add( work );
	}

	@Override
	public void prepare() {
		indexManager.prepare( works.subList( preparedIndex, works.size() ) );
		preparedIndex = works.size();
	}

	@Override
	public CompletableFuture<?> execute() {
		prepare();
		CompletableFuture<?> future = indexManager.execute( works );
		works.clear();
		preparedIndex = 0;
		return future;
	}
}
