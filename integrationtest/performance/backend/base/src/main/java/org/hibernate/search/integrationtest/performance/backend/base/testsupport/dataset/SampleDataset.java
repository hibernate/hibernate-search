/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.performance.backend.base.testsupport.dataset;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.integrationtest.performance.backend.base.testsupport.index.MappedIndex;

import org.openjdk.jmh.annotations.CompilerControl;

@CompilerControl(CompilerControl.Mode.INLINE)
final class SampleDataset implements Dataset {

	private final List<DataSample> samples;

	private final int size;

	SampleDataset(Collection<DataSample> samples) {
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

		private final String shortText;
		private final String longText;
		private final int numeric;

		public DataSample(String shortText, String longText, int numeric) {
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
