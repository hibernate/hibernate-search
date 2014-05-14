/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.checkstyle.checks.regexp;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.puppycrawl.tools.checkstyle.api.FileContents;
import com.puppycrawl.tools.checkstyle.api.FileText;

/**
 * @author Davide D'Alto <davide@hibernate.org>
 */
public class DoubleSpacesCheckTest {

	@Test
	public void testEmptyString() throws Exception {
		DoubleSpacesCheckMock check = new DoubleSpacesCheckMock();
		check.setIgnoreStrings( true );
		check.processLines( Arrays.asList( "" ) );

		Assert.assertTrue( check.violations.isEmpty() );
	}

	@Test
	public void testTwoSpaces() throws Exception {
		DoubleSpacesCheckMock check = new DoubleSpacesCheckMock();
		check.setIgnoreStrings( true );
		check.processLines( Arrays.asList( "  " ) );

		Assert.assertFalse( check.violations.isEmpty() );
	}

	@Test
	public void testTwoSpacesInsideString() throws Exception {
		DoubleSpacesCheckMock check = new DoubleSpacesCheckMock();
		check.setIgnoreStrings( false );
		check.processLines( Arrays.asList( "\"  \"" ) );

		Assert.assertFalse( check.violations.isEmpty() );
	}

	@Test
	public void testTwoSpacesInsideStringWhenStringAreIgnored() throws Exception {
		DoubleSpacesCheckMock check = new DoubleSpacesCheckMock();
		check.setIgnoreStrings( true );
		check.processLines( Arrays.asList( "\"  \"" ) );

		Assert.assertTrue( check.violations.isEmpty() );
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
			FileText fileText = FileText.fromLines( new File( "" ), aLines );
			return new FileContents( fileText );
		}
	}
}
