/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.configuration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Properties;

import org.hibernate.search.exception.SearchException;
import org.hibernate.search.util.configuration.impl.ConfigurationParseHelper;
import org.junit.Test;

/**
 * @author Sanne Grinovero
 */
public class ConfigurationParseHelperTest {

	@Test
	public void testIntegerParsers() {
		assertEquals( 0, ConfigurationParseHelper.parseInt( "   0 ", "not important" ) );
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
