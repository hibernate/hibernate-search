/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.backend.impl.lucene.works;

import org.hibernate.search.backend.AddLuceneWork;
import org.hibernate.search.backend.DeleteLuceneWork;
import org.hibernate.search.backend.FlushLuceneWork;
import org.hibernate.search.backend.OptimizeLuceneWork;
import org.hibernate.search.backend.PurgeAllLuceneWork;
import org.hibernate.search.backend.UpdateLuceneWork;
import org.hibernate.search.backend.impl.WorkVisitor;
import org.hibernate.search.store.Workspace;

/**
 * @author Sanne Grinovero
 */
public class LuceneWorkVisitor implements WorkVisitor<LuceneWorkDelegate> {
	
	private final AddWorkDelegate addDelegate;
	private final DeleteWorkDelegate deleteDelegate;
	private final UpdateWorkDelegate updateDelegate;
	private final OptimizeWorkDelegate optimizeDelegate;
	private final PurgeAllWorkDelegate purgeAllDelegate;
	private final FlushWorkDelegate flushDelegate;
	
	public LuceneWorkVisitor(Workspace workspace) {
		this.addDelegate = new AddWorkDelegate( workspace );
		if ( workspace.areSingleTermDeletesSafe() ) {
			this.deleteDelegate = new DeleteExtWorkDelegate( workspace );
			this.updateDelegate = new UpdateExtWorkDelegate( workspace, addDelegate );
		}
		else {
			this.deleteDelegate = new DeleteWorkDelegate( workspace );
			this.updateDelegate = new UpdateWorkDelegate( deleteDelegate, addDelegate );
		}
		this.purgeAllDelegate = new PurgeAllWorkDelegate( workspace );
		this.optimizeDelegate = new OptimizeWorkDelegate( workspace );
		this.flushDelegate = new FlushWorkDelegate( workspace );
	}

	public LuceneWorkDelegate getDelegate(AddLuceneWork addLuceneWork) {
		return addDelegate;
	}

	public LuceneWorkDelegate getDelegate(DeleteLuceneWork deleteLuceneWork) {
		return deleteDelegate;
	}

	public LuceneWorkDelegate getDelegate(OptimizeLuceneWork optimizeLuceneWork) {
		return optimizeDelegate;
	}

	public LuceneWorkDelegate getDelegate(PurgeAllLuceneWork purgeAllLuceneWork) {
		return purgeAllDelegate;
	}

	public LuceneWorkDelegate getDelegate(UpdateLuceneWork updateLuceneWork) {
		return updateDelegate;
	}

	@Override
	public LuceneWorkDelegate getDelegate(FlushLuceneWork flushLuceneWork) {
		return flushDelegate;
	}

}
