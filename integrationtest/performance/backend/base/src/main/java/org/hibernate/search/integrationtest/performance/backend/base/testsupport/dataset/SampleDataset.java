/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.performance.backend.base.testsupport.dataset;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.integrationtest.performance.backend.base.testsupport.index.MappedIndex;
import org.hibernate.search.util.common.impl.Contracts;

import org.openjdk.jmh.annotations.CompilerControl;

@CompilerControl(CompilerControl.Mode.INLINE)
final class SampleDataset implements Dataset {

	private final List<DataSample> samples;

	private final int size;

	SampleDataset(Collection<DataSample> samples) {
		if ( samples.size() < 10 ) {
			// Just in case something turned wrong during sample generation
			throw new IllegalArgumentException( "Expected 10 samples or more" );
		}
		this.samples = new ArrayList<>( samples );
		this.size = this.samples.size();
	}

	@Override
	public void populate(MappedIndex index, DocumentElement documentElement, long documentId, long randomizer) {
		// Use the the randomizer, which is only based on the document ID, so select the sample.
		// That way, documents updates should actually change indexed data most of the time.
		int sampleIndex = (int) ( ( documentId + randomizer ) % size );
		DataSample sample = samples.get( sampleIndex );
		index.populate(
				documentElement,
				sample.shortText,
				sample.longText,
				sample.numeric
		);
	}

	public static class DataSample {

		final String shortText;
		final String longText;
		final int numeric;

		public DataSample(String shortText, String longText, int numeric) {
			// Just in case something turned wrong during sample generation
			Contracts.assertNotNullNorEmpty( shortText, "shortText" );
			Contracts.assertNotNullNorEmpty( longText, "longText" );
			this.shortText = shortText;
			this.longText = longText;
			this.numeric = numeric;
		}

		@Override
		public String toString() {
			return new StringBuilder()
					.append( getClass().getSimpleName() )
					.append( "<" ).append( shortText ).append( ">" )
					.toString();
		}
	}
}
