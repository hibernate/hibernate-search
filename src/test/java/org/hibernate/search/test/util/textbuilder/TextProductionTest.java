package org.hibernate.search.test.util.textbuilder;

import junit.framework.TestCase;

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
public class TextProductionTest extends TestCase {
	
	public void testSomeWordsGetBuilt() {
		SentenceInventor wi = new SentenceInventor( 7L, 10000 );
		assertEquals( "Qoswo, orrmi ag ybwp bbtb kw qgtqaon lyhk nbv: qrqm flyui hyshm jmpqyb qmolml fjxw gnumocv Twwg.\n", wi.nextPeriod() );
	}

}
