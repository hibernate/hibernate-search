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

import org.hibernate.Criteria;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.StatelessSession;
import org.hibernate.criterion.Projections;

/**
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 *
 */
public class CriteriaLoadingStrategy implements IdentifierLoadingStrategy {
	
	private final Class<?> indexedType;
	
	CriteriaLoadingStrategy(Class<?> indexedType) {
		this.indexedType = indexedType;
	}

	public ScrollableResults getToIndexIdentifiersScrollable(final StatelessSession session) {
		Criteria criteria = session
			.createCriteria( indexedType )
			.setProjection( Projections.id() )
			.setCacheable( false )
			.setFetchSize( 100 );
		return criteria.scroll( ScrollMode.FORWARD_ONLY );
	}
	
	public Number countToBeIndexedEntities(final StatelessSession session) {
		final Number countAsNumber = (Number) session
			.createCriteria( indexedType )
			.setProjection( Projections.rowCount() )
			.setCacheable( false )
			.uniqueResult();
		return countAsNumber;
	}
	
}
