/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.impl;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.document.spi.DocumentState;
import org.hibernate.search.engine.backend.index.spi.ChangesetIndexWorker;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoSessionContext;

/**
 * @author Yoann Rodiere
 */
class ChangesetPojoTypeWorker<D extends DocumentState> extends PojoTypeWorker<D, ChangesetIndexWorker<D>> {

	public ChangesetPojoTypeWorker(PojoTypeManager<?, ?, D> typeManager, PojoSessionContext sessionContext,
			ChangesetIndexWorker<D> delegate) {
		super( typeManager, sessionContext, delegate );
	}

	public CompletableFuture<?> execute() {
		return getDelegate().execute();
	}

}
