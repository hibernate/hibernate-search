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
package org.hibernate.search.query.impl;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.search.engine.EntityInfo;
import org.hibernate.search.engine.SearchFactoryImplementor;
import org.hibernate.search.query.TimeoutManager;
import org.hibernate.search.util.HibernateHelper;
import org.hibernate.search.util.LoggerFactory;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class PersistenceContextObjectsInitializer implements ObjectsInitializer {
	private static final Logger log = LoggerFactory.make();
	private final ObjectsInitializer delegate;

	public PersistenceContextObjectsInitializer(ObjectsInitializer delegate) {
		this.delegate = delegate;
	}

	public void initializeObjects(EntityInfo[] entityInfos,
										 Criteria criteria, Class<?> entityType,
										 SearchFactoryImplementor searchFactoryImplementor,
										 TimeoutManager timeoutManager,
										 Session session) {
		//Do not call isTimeOut here as the caller might be the last biggie on the list.
		final int maxResults = entityInfos.length;
		if ( maxResults == 0 ) {
			log.trace( "No object to initialize", maxResults );
			return;
		}

		//check the persistence context
		List<EntityInfo> remainingEntityInfos = new ArrayList<EntityInfo>( entityInfos.length );
		for ( EntityInfo entityInfo : entityInfos ) {
			final boolean isInitialized = HibernateHelper.isInitialized(
					session.load(
							entityInfo.clazz, entityInfo.id
					)
			);
			if ( !isInitialized ) {
				remainingEntityInfos.add( entityInfo );
			}
		}
		//update entityInfos to only contains the remaining ones
		final int remainingSize = remainingEntityInfos.size();
		log.trace( "Initialized {} objects out of {} in the persistence contedt", maxResults - remainingSize, maxResults );
		if (remainingSize > 0) {
			delegate.initializeObjects(
					remainingEntityInfos.toArray( new EntityInfo[remainingSize] ),
					criteria,
					entityType,
					searchFactoryImplementor,
					timeoutManager,
					session
			);
		}
	}
}
