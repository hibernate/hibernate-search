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
package org.hibernate.search.test.util.textbuilder;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests WordDictionary and WordInventor,
 * these are test utilities not part of the Search distribution;
 * the test exists to spot if the text they produce is unchanged, so
 * that other tests can rely on working test utilities.
 *
 * @see WordDictionary
 * @see SentenceInventor
 *
 * @author Sanne Grinovero
 */
public class TextProductionTest {

	@Test
	public void testSomeWordsGetBuilt() {
		SentenceInventor wi = new SentenceInventor( 7L, 200 );
		String randomPeriod = wi.nextPeriod();
		// randomPeriod will be some random sentence like "Qoswo, orrmi ag ybwp bbtb kw qgtqaon lyhk nbv: qrqm flyui hyshm jmpqyb qmolml fjxw gnumocv Twwg."
		// but exact string contents depends on environment
		assertNotNull( randomPeriod );
		assertTrue( randomPeriod.length() > 0 );
	}

}
