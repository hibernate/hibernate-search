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
package org.hibernate.search.test.performance.reader;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.hibernate.SessionFactory;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;

/**
 * @author Sanne Grinovero
 */
public class UpdateActivity extends AbstractActivity {

	UpdateActivity(SessionFactory sf, CountDownLatch startSignal) {
		super( sf, startSignal );
	}

	@Override
	protected void doAction(FullTextSession s, int jobSeed) {
		FullTextQuery q = getQuery( "John", s, Detective.class );
		List list = q.setMaxResults( 1 ).list();
		for ( Object o : list ) {
			Detective detective = (Detective) o;
			detective.setPhysicalDescription( "old" );
		}
	}
}
