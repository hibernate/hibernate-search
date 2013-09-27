/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.search.backend.impl.lucene;

import java.util.Properties;

import org.hibernate.search.indexes.impl.PropertiesParseHelper;
import org.hibernate.search.indexes.impl.DirectoryBasedIndexManager;
import org.hibernate.search.spi.WorkerBuildContext;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
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
