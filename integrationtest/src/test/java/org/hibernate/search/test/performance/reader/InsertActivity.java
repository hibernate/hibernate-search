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

import java.util.concurrent.CountDownLatch;

import org.hibernate.SessionFactory;
import org.hibernate.search.FullTextSession;

/**
 * @author Emmanuel Bernard
 * @author Sanne Grinovero
 */
public class InsertActivity extends AbstractActivity {

	InsertActivity(SessionFactory sf, CountDownLatch startSignal) {
		super( sf, startSignal );
	}

	@Override
	protected void doAction(FullTextSession s, int jobSeed) {
		Detective detective = new Detective();
		detective.setName( "John Doe " + jobSeed );
		detective.setBadge( "123455" + jobSeed );
		detective.setPhysicalDescription( "Blond green eye etc etc" );
		s.persist( detective );
		Suspect suspect = new Suspect();
		suspect.setName( "Jane Doe " + jobSeed );
		suspect.setPhysicalDescription( "brunette, short, 30-ish" );
		if ( jobSeed % 20 == 0 ) {
			suspect.setSuspectCharge( "thief liar " );
		}
		else {
			suspect.setSuspectCharge(
					" It's 1875 in London. The police have captured career criminal Montmorency. In the process he has been grievously wounded and it is up to a young surgeon to treat his wounds. During his recovery Montmorency learns of the city's new sewer system and sees in it the perfect underground highway for his thievery.  Washington Post columnist John Kelly recommends this title for middle schoolers, especially to be read aloud."
			);
		}
		s.persist( suspect );
	}
}
