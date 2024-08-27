/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.performance.backend.base.testsupport.dataset;

import java.util.ArrayList;
import java.util.List;

final class SimpleDataSampleParser {

	private static final int LINES_PER_SAMPLE = 10;

	private final List<SampleDataset.DataSample> result = new ArrayList<>();

	private int currentSampleId = 0;
	private int currentLineCount = 0;
	private String currentSampleFirstLine = null;
	private final StringBuilder currentSampleWholeText = new StringBuilder();

	public boolean processLine(String line) {
		if ( line.isEmpty() ) {
			return true;
		}
		if ( currentLineCount == 0 ) {
			currentSampleFirstLine = line;
		}
		else {
			currentSampleWholeText.append( "\n" );
		}
		currentSampleWholeText.append( line );
		++currentLineCount;
		if ( currentLineCount >= LINES_PER_SAMPLE ) {
			pushCurrentSample();
		}
		return true;
	}

	public List<SampleDataset.DataSample> getResult() {
		pushCurrentSample();
		return result;
	}

	private void pushCurrentSample() {
		if ( currentLineCount == 0 ) {
			return;
		}

		result.add( new SampleDataset.DataSample(
				currentSampleFirstLine, currentSampleWholeText.toString(), currentSampleId
		) );
		++this.currentSampleId;

		currentLineCount = 0;
		currentSampleFirstLine = null;
		currentSampleWholeText.setLength( 0 );
	}

}
