/* $Id$
 * 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * Copyright (c) 2009, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
package org.hibernate.search.event;

import org.hibernate.event.PostCollectionRecreateEvent;
import org.hibernate.event.PostCollectionRecreateEventListener;
import org.hibernate.event.PostCollectionRemoveEvent;
import org.hibernate.event.PostCollectionRemoveEventListener;
import org.hibernate.event.PostCollectionUpdateEvent;
import org.hibernate.event.PostCollectionUpdateEventListener;

/**
 * @author Emmanuel Bernard
 * @deprecated As of release 3.1.0, replaced by {@link FullTextIndexEventListener}
 */
@SuppressWarnings("serial")
@Deprecated 
public class FullTextIndexCollectionEventListener extends FullTextIndexEventListener
		implements PostCollectionRecreateEventListener,
		PostCollectionRemoveEventListener,
		PostCollectionUpdateEventListener {

	/**
	 * @deprecated As of release 3.1.0, replaced by {@link FullTextIndexEventListener#onPostRecreateCollection(PostCollectionRecreateEvent)}
	 */
	@Deprecated 	
	public void onPostRecreateCollection(PostCollectionRecreateEvent event) {
		processCollectionEvent( event );
	}

	/**
	 * @deprecated As of release 3.1.0, replaced by {@link FullTextIndexEventListener#onPostRemoveCollection(PostCollectionRemoveEvent)}
	 */
	@Deprecated 	
	public void onPostRemoveCollection(PostCollectionRemoveEvent event) {
		processCollectionEvent( event );
	}

	/**
	 * @deprecated As of release 3.1.0, replaced by {@link FullTextIndexEventListener#onPostUpdateCollection(PostCollectionUpdateEvent)}
	 */
	@Deprecated 	
	public void onPostUpdateCollection(PostCollectionUpdateEvent event) {
		processCollectionEvent( event );
	}
}
