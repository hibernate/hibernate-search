// $Id$
package org.hibernate.search.test.configuration;

import java.util.Properties;

import junit.framework.TestCase;

import org.hibernate.search.SearchException;
import org.hibernate.search.backend.configuration.ConfigurationParseHelper;

/**
 * @author Sanne Grinovero
 */
public class ConfigurationParseHelperTest extends TestCase {
	
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
		} catch (SearchException e) {
			exceptionLaunched = true;
		}
		assertTrue( exceptionLaunched );
	}
	
	public void testBooleanParsers() {
		assertTrue( ConfigurationParseHelper.parseBoolean( "true", null ) );
		assertTrue( ConfigurationParseHelper.parseBoolean( " True ", null ) );
		assertFalse( ConfigurationParseHelper.parseBoolean( "false", null ) );
		assertFalse( ConfigurationParseHelper.parseBoolean( " False  ", null ) );
		boolean exceptionLaunched = false;
		try {
			ConfigurationParseHelper.parseBoolean( "5", "error" );
		} catch (SearchException e) {
			exceptionLaunched = true;
		}
		assertTrue( exceptionLaunched );
		exceptionLaunched = false;
		try {
			ConfigurationParseHelper.parseBoolean( null, "error" );
		} catch (SearchException e) {
			exceptionLaunched = true;
		}
		assertTrue( exceptionLaunched );
	}

}
