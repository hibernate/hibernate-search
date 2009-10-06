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
package org.hibernate.search.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.search.util.LoggerFactory;

/**
 * @author Emmanuel Bernard
 */
public class ObjectLoader implements Loader {
	private static final Logger log = LoggerFactory.make();
	private Session session;

	public void init(Session session, SearchFactoryImplementor searchFactoryImplementor) {
		this.session = session;
	}

	public Object load(EntityInfo entityInfo) {
		return ObjectLoaderHelper.load( entityInfo, session );
	}

	public List load(EntityInfo... entityInfos) {
		if ( entityInfos.length == 0 ) return Collections.EMPTY_LIST;
		if ( entityInfos.length == 1 ) {
			final Object entity = load( entityInfos[0] );
			if ( entity == null ) {
				return Collections.EMPTY_LIST;
			}
			else {
				final List<Object> list = new ArrayList<Object>( 1 );
				list.add( entity );
				return list;
			}
		}
		//use load to benefit from the batch-size
		//we don't face proxy casting issues since the exact class is extracted from the index
		for (EntityInfo entityInfo : entityInfos) {
			session.load( entityInfo.clazz, entityInfo.id );
		}
		List result = new ArrayList( entityInfos.length );
		for (EntityInfo entityInfo : entityInfos) {
			try {
				Object entity = session.load( entityInfo.clazz, entityInfo.id );
				Hibernate.initialize( entity );
				result.add( entity );
			}
			catch (RuntimeException e) {
				if ( LoaderHelper.isObjectNotFoundException( e ) ) {
					log.debug( "Object found in Search index but not in database: {} with id {}",
							entityInfo.clazz, entityInfo.id );
				}
				else {
					throw e;
				}
			}
		}
		return result;
	}
}
