/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.checkstyle.checks.regexp;

import com.puppycrawl.tools.checkstyle.api.FileContents;

/**
 * A MatchSuppressor used to suppress a violation if it happens inside a {@link String}.
 *
 * @author Davide D'Alto
 */
public class StringSuppressor implements MatchSuppressor {

	private FileContents fileContents;

	@Override
	public boolean shouldSuppress(int aStartLineNo, int aStartColNo, int aEndLineNo, int aEndColNo) {
		return isInsideString( fileContents.getLines()[aStartLineNo - 1], aStartColNo, aEndColNo );
	}

	public void setCurrentContents(FileContents fileContents) {
		this.fileContents = fileContents;
	}

	/**
	 * Count the quotes before the double spaces and after, if one of the result is odd it means there is an open double
	 * quote. Do the same for the part of the text after the spaces.
	 * <p>
	 * This function is very simple and it works because we are looking for whitespace characters.
	 *
	 * @param line
	 *            the full line of text containing the part that we want to check
	 * @param aStartColNo
	 *            position of the first white space
	 * @param aEndColNo
	 *            position of the second white space
	 * @return true if the two white spaces are inside a string (between quotes)
	 */
	private boolean isInsideString(String line, int aStartColNo, int aEndColNo) {
		if ( isEmpty( line ) ) {
			return false;
		}
		else {
			String token = line.substring( 0, aStartColNo );
			int before = countQuotes( token );
			token = line.substring( aEndColNo + 1 );
			int after = countQuotes( token );
			return odd( before ) || odd( after );
		}
	}

	private boolean isEmpty(String line) {
		return line == null || line.isEmpty();
	}

	private boolean odd(int num) {
		return num % 2 != 0.;
	}

	private int countQuotes(String token) {
		int quoteNumber = 0;
		for ( int i = 0; i < token.length(); i++ ) {
			if ( token.charAt( i ) == '"' ) {
				if ( i == 0 || notEscaped( token, i ) ) {
					quoteNumber++;
				}
			}
		}
		return quoteNumber;
	}

	private boolean notEscaped(String token, int i) {
		return i > 0 && token.charAt( i - 1 ) != '\\';
	}
}
