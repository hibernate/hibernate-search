/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.testsupport.textbuilder;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

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
class TextProductionTest {

	@Test
	void testSomeWordsGetBuilt() {
		SentenceInventor wi = new SentenceInventor( 7L, 200 );
		String randomPeriod = wi.nextPeriod();
		// randomPeriod will be some random sentence like "Qoswo, orrmi ag ybwp bbtb kw qgtqaon lyhk nbv: qrqm flyui hyshm jmpqyb qmolml fjxw gnumocv Twwg."
		// but exact string contents depends on environment
		assertThat( randomPeriod ).isNotNull();
		assertThat( randomPeriod ).isNotEmpty();
	}

}
