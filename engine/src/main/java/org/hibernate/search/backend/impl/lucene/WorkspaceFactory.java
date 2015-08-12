/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.impl.lucene;

import java.util.Properties;

import org.hibernate.search.indexes.impl.PropertiesParseHelper;
import org.hibernate.search.indexes.spi.DirectoryBasedIndexManager;
import org.hibernate.search.spi.WorkerBuildContext;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * @author Sanne Grinovero (C) 2011 Red Hat Inc.
 */
final class WorkspaceFactory {

	private static final Log log = LoggerFactory.make();

	private WorkspaceFactory() {
		//not allowed
	}

	static AbstractWorkspaceImpl createWorkspace(DirectoryBasedIndexManager indexManager,
			WorkerBuildContext context, Properties cfg) {
		final String indexName = indexManager.getIndexName();
		final boolean exclusiveIndexUsage = PropertiesParseHelper.isExclusiveIndexUsageEnabled( cfg );
		if ( exclusiveIndexUsage ) {
			log.debugf( "Starting workspace for index " + indexName + " using an exclusive index strategy" );
			return new ExclusiveIndexWorkspaceImpl( indexManager, context, cfg );
		}
		else {
			log.debugf( "Starting workspace for index " + indexName + " using a shared index strategy" );
			return new SharedIndexWorkspaceImpl( indexManager, context, cfg );
		}
	}

}
