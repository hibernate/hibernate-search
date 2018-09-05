/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.impl.lucene;

import org.hibernate.search.backend.BackendFactory;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.impl.CommitPolicy;
import org.hibernate.search.exception.ErrorHandler;
import org.hibernate.search.indexes.impl.PropertiesParseHelper;
import org.hibernate.search.indexes.spi.DirectoryBasedIndexManager;
import org.hibernate.search.spi.WorkerBuildContext;

import java.util.Properties;

/**
 * @author Sanne Grinovero (C) 2011 Red Hat Inc.
 */
public class ExclusiveIndexWorkspaceImpl extends AbstractWorkspaceImpl {

	private final CommitPolicy commitPolicy;

	public ExclusiveIndexWorkspaceImpl(DirectoryBasedIndexManager indexManager, WorkerBuildContext context, Properties cfg) {
		super( indexManager, context, cfg );
		boolean async = ! BackendFactory.isConfiguredAsSync( cfg );
		if ( async ) {
			int commitInterval = PropertiesParseHelper.extractFlushInterval( indexManager.getIndexName(), cfg );
			ErrorHandler errorHandler = context.getErrorHandler();
			commitPolicy = new ScheduledCommitPolicy( writerHolder, indexManager.getIndexName(), commitInterval, errorHandler );
		}
		else {
			commitPolicy = new PerChangeSetCommitPolicy( writerHolder );
		}
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
