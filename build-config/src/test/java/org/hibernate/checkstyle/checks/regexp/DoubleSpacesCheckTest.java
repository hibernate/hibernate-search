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
