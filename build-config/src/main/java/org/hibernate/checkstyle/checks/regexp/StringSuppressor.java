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

import com.puppycrawl.tools.checkstyle.api.FileContents;

/**
 * {@link com.puppycrawl.tools.checkstyle.checks.regexp.MatchSuppressor} used to suppress a violation if it happens inside a {@link String}.
 *
 * @author Davide D'Alto <davide@hibernate.org>
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
