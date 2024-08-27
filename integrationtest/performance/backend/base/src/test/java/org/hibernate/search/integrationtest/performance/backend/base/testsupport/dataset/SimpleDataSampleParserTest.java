/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.performance.backend.base.testsupport.dataset;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.hibernate.search.util.common.annotation.impl.SuppressJQAssistant;

import org.junit.jupiter.api.Test;

@SuppressJQAssistant(reason = "This really is a unit test, not an IT, so we want the 'Test' suffix")
class SimpleDataSampleParserTest {

	@Test
	void test() {
		SimpleDataSampleParser parser = new SimpleDataSampleParser();
		( "\n"
				+ "This is the first real line\n"
				+ "Followed by another one\n"
				+ "Then a few empty lines:\n"
				+ "\n\n\n\n\n\n\n\n\n\n"
				+ "Then line 4\n"
				+ "Then line 5\n"
				+ "Then line 6\n"
				+ "Then line 7\n"
				+ "Then line 8\n"
				+ "Then line 9\n"
				+ "\n"
				+ "Then line 10\n"
				+ "This is the first line of the next sample" )
				.lines()
				.forEach( parser::processLine );

		List<SampleDataset.DataSample> samples = parser.getResult();

		assertThat( samples ).hasSize( 2 );

		assertThat( samples.get( 0 ).shortText ).isEqualTo( "This is the first real line" );
		assertThat( samples.get( 0 ).longText ).isEqualTo( "This is the first real line\n"
				+ "Followed by another one\n"
				+ "Then a few empty lines:\n"
				+ "Then line 4\n"
				+ "Then line 5\n"
				+ "Then line 6\n"
				+ "Then line 7\n"
				+ "Then line 8\n"
				+ "Then line 9\n"
				+ "Then line 10" );
		assertThat( samples.get( 0 ).numeric ).isEqualTo( 0L );

		assertThat( samples.get( 1 ).shortText ).isEqualTo( "This is the first line of the next sample" );
		assertThat( samples.get( 1 ).longText ).isEqualTo( "This is the first line of the next sample" );
		assertThat( samples.get( 1 ).numeric ).isEqualTo( 1L );
	}

}
