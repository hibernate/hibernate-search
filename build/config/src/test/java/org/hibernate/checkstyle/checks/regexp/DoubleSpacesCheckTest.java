/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.checkstyle.checks.regexp;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.puppycrawl.tools.checkstyle.api.FileContents;
import com.puppycrawl.tools.checkstyle.api.FileText;

/**
 * @author Davide D'Alto
 */
class DoubleSpacesCheckTest {

	@Test
	void testEmptyString() throws Exception {
		DoubleSpacesCheckMock check = new DoubleSpacesCheckMock();
		check.setIgnoreStrings( true );
		check.processLines( Arrays.asList( "" ) );

		assertThat( check.violations ).isEmpty();
	}

	@Test
	void testTwoSpaces() throws Exception {
		DoubleSpacesCheckMock check = new DoubleSpacesCheckMock();
		check.setIgnoreStrings( true );
		check.processLines( Arrays.asList( "  " ) );

		assertThat( check.violations ).isNotEmpty();
	}

	@Test
	void testTwoSpacesInsideString() throws Exception {
		DoubleSpacesCheckMock check = new DoubleSpacesCheckMock();
		check.setIgnoreStrings( false );
		check.processLines( Arrays.asList( "\"  \"" ) );

		assertThat( check.violations ).isNotEmpty();
	}

	@Test
	void testTwoSpacesInsideStringWhenStringAreIgnored() throws Exception {
		DoubleSpacesCheckMock check = new DoubleSpacesCheckMock();
		check.setIgnoreStrings( true );
		check.processLines( Arrays.asList( "\"  \"" ) );

		assertThat( check.violations ).isEmpty();
	}

	private static class DoubleSpacesCheckMock extends DoubleSpacesCheck {
		List<Integer> violations = new ArrayList<Integer>();

		@Override
		public void processLines(List<String> aLines) {
			super.initializeSuppressors( content( aLines ) );
			super.processLines( aLines );
		}

		@Override
		protected void handleViolation(int aLineno, String aLine, int startCol) {
			violations.add( aLineno );
		}

		private FileContents content(List<String> aLines) {
			FileText fileText = new FileText( new File( "" ), aLines );
			return new FileContents( fileText );
		}
	}
}
