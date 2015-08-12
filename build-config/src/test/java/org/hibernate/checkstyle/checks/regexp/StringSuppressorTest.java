/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.checkstyle.checks.regexp;

import java.io.File;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import com.puppycrawl.tools.checkstyle.api.FileContents;
import com.puppycrawl.tools.checkstyle.api.FileText;

/**
 * @author Davide D'Alto
 */
public class StringSuppressorTest {

	@Test
	public void testOutsideString() throws Exception {
		StringSuppressor suppressor = new StringSuppressor();
		suppressor.setCurrentContents( content( "  " ) );
		Assert.assertFalse( suppressor.shouldSuppress( 1, 0, 0, 1 ) );
	}

	@Test
	public void testInsideString() throws Exception {
		StringSuppressor suppressor = new StringSuppressor();
		suppressor.setCurrentContents( content( "\"           \"" ) );
		Assert.assertTrue( suppressor.shouldSuppress( 1, 4, 0, 6 ) );
	}

	@Test
	public void testInsideStringWithText() throws Exception {
		StringSuppressor suppressor = new StringSuppressor();
		suppressor.setCurrentContents( content( "\"text   text\"" ) );
		Assert.assertTrue( suppressor.shouldSuppress( 1, 4, 0, 11 ) );
	}

	@Test
	public void testInsideStringWithCode() throws Exception {
		StringSuppressor suppressor = new StringSuppressor();
		suppressor.setCurrentContents( content( "System.out.println(\"text   text\");" ) );
		Assert.assertTrue( suppressor.shouldSuppress( 1, 25, 0, 27 ) );
	}

	@Test
	public void testOutsideStringWithCode() throws Exception {
		StringSuppressor suppressor = new StringSuppressor();
		suppressor.setCurrentContents( content( "System.out.println   (\"text text\");" ) );
		Assert.assertFalse( suppressor.shouldSuppress( 1, 18, 0, 20 ) );
	}

	private FileContents content(String string) {
		FileText fileText = FileText.fromLines( new File( "" ), Arrays.asList( string ) );
		return new FileContents( fileText );
	}
}
