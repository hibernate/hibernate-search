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

import org.hibernate.search.backend.BackendFactory;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.impl.CommitPolicy;
import org.hibernate.search.indexes.impl.DirectoryBasedIndexManager;
import org.hibernate.search.indexes.impl.PropertiesParseHelper;
import org.hibernate.search.spi.WorkerBuildContext;

import java.util.Properties;

/**
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class ExclusiveIndexWorkspaceImpl extends AbstractWorkspaceImpl {

	private final CommitPolicy commitPolicy;

	public ExclusiveIndexWorkspaceImpl(DirectoryBasedIndexManager indexManager, WorkerBuildContext context, Properties cfg) {
		super( indexManager, context, cfg );
		boolean async = ! BackendFactory.isConfiguredAsSync( cfg );
		if ( async ) {
			int commitInterval = PropertiesParseHelper.extractFlushInterval( indexManager.getIndexName(), cfg );
			commitPolicy = new ScheduledCommitPolicy( writerHolder, indexManager.getIndexName(), commitInterval );
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
