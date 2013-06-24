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
package com.puppycrawl.tools.checkstyle.checks.regexp;

import java.io.File;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import com.puppycrawl.tools.checkstyle.api.FileContents;
import com.puppycrawl.tools.checkstyle.api.FileText;

/**
 * @author Davide D'Alto <davide@hibernate.org>
 */
public class StringSuppressorTest {

	@Test
	public void testOutsideString() throws Exception {
		StringSuppressor suppressor = new StringSuppressor();
		suppressor.setCurrentContents( content( "  " ) );
		Assert.assertFalse( suppressor.shouldSuppress( 0, 0, 0, 1 ) );
	}

	@Test
	public void testInsideString() throws Exception {
		StringSuppressor suppressor = new StringSuppressor();
		suppressor.setCurrentContents( content( "\"           \"" ) );
		Assert.assertTrue( suppressor.shouldSuppress( 0, 4, 0, 6 ) );
	}

	private FileContents content(String string) {
		FileText fileText = FileText.fromLines( new File( "" ), Arrays.asList( string ) );
		return new FileContents( fileText );
	}
}
