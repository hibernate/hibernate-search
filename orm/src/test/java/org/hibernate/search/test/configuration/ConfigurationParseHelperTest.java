/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.test.configuration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Properties;

import org.hibernate.search.SearchException;
import org.hibernate.search.util.configuration.impl.ConfigurationParseHelper;
import org.junit.Test;

/**
 * @author Sanne Grinovero
 */
public class ConfigurationParseHelperTest {

	@Test
	public void testIntegerParsers() {
		assertEquals( 0, ConfigurationParseHelper.parseInt( "   0 ", "not important") );
		assertEquals( 8, ConfigurationParseHelper.parseInt( null, 8, null ) );
		assertEquals( 56, ConfigurationParseHelper.parseInt( "56", 8, null ) );
		Properties props = new Properties();
		props.setProperty( "value1", "58" );
		assertEquals( 58, ConfigurationParseHelper.getIntValue( props, "value1", 8 ) );
		assertEquals( 8, ConfigurationParseHelper.getIntValue( props, "value2", 8 ) );
		props.setProperty( "value2", "nand" );
		boolean exceptionLaunched = false;
		try {
			ConfigurationParseHelper.getIntValue( props, "value2", 8 );
		}
		catch (SearchException e) {
			exceptionLaunched = true;
		}
		assertTrue( exceptionLaunched );
	}

	@Test
	public void testBooleanParsers() {
		assertTrue( ConfigurationParseHelper.parseBoolean( "true", null ) );
		assertTrue( ConfigurationParseHelper.parseBoolean( " True ", null ) );
		assertFalse( ConfigurationParseHelper.parseBoolean( "false", null ) );
		assertFalse( ConfigurationParseHelper.parseBoolean( " False  ", null ) );
		boolean exceptionLaunched = false;
		try {
			ConfigurationParseHelper.parseBoolean( "5", "error" );
		}
		catch (SearchException e) {
			exceptionLaunched = true;
		}
		assertTrue( exceptionLaunched );
		exceptionLaunched = false;
		try {
			ConfigurationParseHelper.parseBoolean( null, "error" );
		}
		catch (SearchException e) {
			exceptionLaunched = true;
		}
		assertTrue( exceptionLaunched );
	}

}
