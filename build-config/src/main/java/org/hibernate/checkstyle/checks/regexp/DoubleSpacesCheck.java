/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.checkstyle.checks.regexp;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.puppycrawl.tools.checkstyle.api.Check;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.FileContents;

/**
 * Check if the code contains two consecutive whitespace characters.
 * <p>
 * This check considers a whitespace characters only the one generated with the space bar button.
 * All the other whitespace characters are not included ( example: \t\n\x0B\f\r).
 *
 * @author Davide D'Alto <davide@hibernate.org>
 */
public class DoubleSpacesCheck extends Check {

	private static final String WHITESPACE_DOUBLE_KEY = "whitespace.double";
	private static final Pattern TWO_SPACES_PATTERN = Pattern.compile( "  " );

	private final CommentSuppressor commentSuppressor = new CommentSuppressor();
	private final StringSuppressor stringSuppressor = new StringSuppressor();
	private final Set<MatchSuppressor> suppressors = new HashSet<MatchSuppressor>();

	@Override
	public int[] getDefaultTokens() {
		return new int[0];
	}

	@Override
	public void beginTree(DetailAST aRootAST) {
		initializeSuppressors( getFileContents() );
		processLines( Arrays.asList( getLines() ) );
	}

	void initializeSuppressors(FileContents fileContents) {
		stringSuppressor.setCurrentContents( fileContents );
		commentSuppressor.setCurrentContents( fileContents );
	}

	/**
	 * Process a set of lines looking for matches.
	 *
	 * @param aLines the lines to process.
	 */
	public void processLines(List<String> aLines) {
		int lineno = 0;
		for ( String line : aLines ) {
			lineno++;
			checkLine( lineno, line, TWO_SPACES_PATTERN.matcher( line ), 0 );
		}
	}

	private void checkLine(int aLineNo, String aLine, Matcher aMatcher, int aStartPosition) {
		final boolean foundMatch = aMatcher.find( aStartPosition );
		if ( foundMatch ) {
			final int startCol = aMatcher.start( 0 );
			final int endCol = aMatcher.end( 0 );

			if ( suppressViolation( aLineNo, startCol, endCol - 1 ) ) {
				if ( endCol < aLine.length() ) {
					// check if the expression is on the rest of the line
					checkLine( aLineNo, aLine, aMatcher, endCol );
				}
			}
			else {
				handleViolation( aLineNo, aLine, startCol );
			}
		}
	}

	private boolean suppressViolation(int aLineNo, int startCol, int endCol) {
		for ( MatchSuppressor suppressor : suppressors ) {
			if ( suppressor.shouldSuppress( aLineNo, startCol, aLineNo, endCol ) ) {
				return true;
			}
		}
		return false;
	}

	protected void handleViolation(int aLineNo, String aLine, final int startCol) {
		log( aLineNo, startCol, WHITESPACE_DOUBLE_KEY, aLine );
	}

	/**
	 * Whether or not comments should be checked.
	 *
	 * @param ignoreComments if true, comments won't be checked
	 */
	public void setIgnoreComments(boolean ignoreComments) {
		if ( ignoreComments ) {
			suppressors.add( commentSuppressor );
		}
		else {
			suppressors.add( NeverSuppress.INSTANCE );
		}
	}

	/**
	 * Whether or not {@link String} content should be checked.
	 *
	 * @param ignoreStrings if true, double spaces inside a String won't be checked
	 */
	public void setIgnoreStrings(boolean ignoreStrings) {
		if ( ignoreStrings ) {
			suppressors.add( stringSuppressor );
		}
		else {
			suppressors.add( NeverSuppress.INSTANCE );
		}
	}
}
