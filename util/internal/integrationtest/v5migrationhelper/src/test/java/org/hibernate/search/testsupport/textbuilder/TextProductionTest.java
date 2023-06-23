/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.testsupport.textbuilder;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

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
