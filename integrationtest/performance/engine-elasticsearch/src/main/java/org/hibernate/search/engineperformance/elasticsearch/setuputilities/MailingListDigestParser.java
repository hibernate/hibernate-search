/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engineperformance.elasticsearch.setuputilities;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hibernate.search.engineperformance.elasticsearch.datasets.TextSampleDataset.TextSample;

final class MailingListDigestParser {

	private final BufferedReader reader;

	private final List<TextSample> result = new ArrayList<>();

	private String currentSampleSubject = null;

	private final StringBuilder currentSampleText = new StringBuilder();

	private final List<String> peekBuffer = new ArrayList<>();

	private State state = State.INITIAL;

	public MailingListDigestParser(BufferedReader reader) {
		this.reader = reader;
	}

	public List<TextSample> parse() throws IOException {
		String line = reader.readLine();
		while ( line != null ) {
			peekBuffer.add( line );
			state = state.parseLine( this, line );
			line = reader.readLine();
		}
		flushPeekBuffer();
		pushCurrentSample();
		return result;
	}

	private void flushPeekBuffer() {
		for ( String line : peekBuffer ) {
			currentSampleText.append( line ).append( "\n" );
		}
		peekBuffer.clear();
	}

	public void setCurrentSampleSubject(String currentSampleSubject) {
		this.currentSampleSubject = currentSampleSubject;
	}

	private void pushCurrentSample() {
		result.add( new TextSample( currentSampleSubject, currentSampleText.toString() ) );
		this.currentSampleSubject = null;
		this.currentSampleText.setLength( 0 );
	}

	private abstract static class State {

		private static final State INITIAL = new State( Pattern.compile( "^From " ) ) {
			@Override
			protected State nextState(MailingListDigestParser parser, Matcher matcher) {
				return AFTER_FROM_1;
			}
		};

		private static final State AFTER_FROM_1 = new State( Pattern.compile( "^From: " ) ) {
			@Override
			protected State nextState(MailingListDigestParser parser, Matcher matcher) {
				return AFTER_FROM_2;
			}
		};

		private static final State AFTER_FROM_2 = new State( Pattern.compile( "^Date: " ) ) {
			@Override
			protected State nextState(MailingListDigestParser parser, Matcher matcher) {
				return AFTER_DATE;
			}
		};

		private static final State AFTER_DATE = new State( Pattern.compile( "^Subject: (.*)" ) ) {
			@Override
			protected State nextState(MailingListDigestParser parser, Matcher matcher) {
				parser.pushCurrentSample();
				parser.flushPeekBuffer();
				parser.setCurrentSampleSubject( matcher.group( 1 ) );
				return INITIAL;
			}
		};

		private final Pattern pattern;

		private State(Pattern pattern) {
			this.pattern = pattern;
		}

		public State parseLine(MailingListDigestParser parser, String line) {
			Matcher matcher = pattern.matcher( line );
			if ( matcher.find() ) {
				return nextState( parser, matcher );
			}
			else {
				parser.flushPeekBuffer();
				return State.INITIAL;
			}
		}

		protected abstract State nextState(MailingListDigestParser parser, Matcher matcher);
	}

}
