/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.impl.lucene;

import org.apache.lucene.index.IndexWriter;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.impl.CommitPolicy;
import org.hibernate.search.exception.impl.ErrorContextBuilder;
import org.hibernate.search.indexes.spi.DirectoryBasedIndexManager;
import org.hibernate.search.spi.WorkerBuildContext;

import java.util.Properties;

/**
 * @author Sanne Grinovero (C) 2011 Red Hat Inc.
 */
public class SharedIndexWorkspaceImpl extends AbstractWorkspaceImpl {

	private final CommitPolicy commitPolicy = new SharedIndexCommitPolicy( writerHolder );

	public SharedIndexWorkspaceImpl(DirectoryBasedIndexManager indexManager, WorkerBuildContext context, Properties cfg) {
		super( indexManager, context, cfg );
	}

	@Override
	public IndexWriter getIndexWriter() {
		return commitPolicy.getIndexWriter();
	}

	@Override
	public IndexWriter getIndexWriter(ErrorContextBuilder errorContextBuilder) {
		return commitPolicy.getIndexWriter( errorContextBuilder );
	}

	@Override
	public void notifyWorkApplied(LuceneWork work) {
		incrementModificationCounter();
	}

	@Override
	public CommitPolicy getCommitPolicy() {
		return commitPolicy;
	}

}
