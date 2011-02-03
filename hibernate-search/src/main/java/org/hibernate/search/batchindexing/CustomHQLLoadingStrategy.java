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

import java.util.Map;

import org.hibernate.Query;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.StatelessSession;
import org.hibernate.search.SearchException;

/**
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 *
 */
public class CustomHQLLoadingStrategy implements IdentifierLoadingStrategy {
	
	private final String countQueryHQL;
	private final String idLoadingHQL;
	private final Map<String,Object> customQueryParameters;
	
	public CustomHQLLoadingStrategy(String countQueryHQL, String idLoadingHQL, Map<String,Object> customQueryParameters) {
		this.countQueryHQL = countQueryHQL;
		this.idLoadingHQL = idLoadingHQL;
		this.customQueryParameters = customQueryParameters;
	}
	
	public ScrollableResults getToIndexIdentifiersScrollable(final StatelessSession session) {
		Query idLoadingQuery = session.createQuery( idLoadingHQL );
		applyQueryParameters( idLoadingQuery );
		return idLoadingQuery.scroll( ScrollMode.FORWARD_ONLY );
	}
	
	public Number countToBeIndexedEntities(final StatelessSession session) {
		Query countingQuery = session.createQuery( countQueryHQL );
		applyQueryParameters( countingQuery );
		Object result = countingQuery.uniqueResult();
		if (result instanceof Number) {
			return (Number) result; 
		}
		else {
			throw new SearchException( "The countQueryHQL is not returning a Number: '" + countQueryHQL + "'" );
		}
	}
	
	public void applyQueryParameters(Query query) {
		for ( String parameterName : query.getNamedParameters() ) {
			Object parameterValue = getParameterValue( parameterName );
			query.setParameter( parameterName, parameterValue );
		}
	}
	
	private Object getParameterValue(String parameterName) {
		if ( ! this.customQueryParameters.containsKey( parameterName ) ) {
			throw new SearchException( "Required parameter missing: " + parameterName );
		}
		return this.customQueryParameters.get( parameterName );
	}
	
}
