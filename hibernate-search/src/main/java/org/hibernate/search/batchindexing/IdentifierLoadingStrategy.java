/* 
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

package org.hibernate.search.batchindexing;

import org.hibernate.ScrollableResults;
import org.hibernate.StatelessSession;
import org.hibernate.search.MassIndexer;

/**
 * <p>Implementing a custom IdentifierLoadingStrategy you get full control on how
 * and which entities are selected for indexing in the MassIndexer.</p>
 * <p>It is not required to implement such a strategy, if none is provided either
 * CriteriaLoadingStrategy or CustomHQLLoadingStrategy will be used; the latter
 * if any custom HQL is provided.</p>
 * 
 * @see MassIndexer#idLoadingStrategy(IdentifierLoadingStrategy)
 * @see MassIndexer#countQuery(String)
 * @see MassIndexer#primaryKeySelectingQuery(String)
 * @see CriteriaLoadingStrategy
 * @see CustomHQLLoadingStrategy
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public interface IdentifierLoadingStrategy {
	
	/**
	 * @param session
	 * @return a ScrollableResults iterating on all primary keys of the entities being indexed. 
	 */
	ScrollableResults getToIndexIdentifiersScrollable(final StatelessSession session);
	
	/**
	 * @param session
	 * @return the Number representing the count(*) of all entities being indexed.
	 */
	Number countToBeIndexedEntities(final StatelessSession session);
	
}
