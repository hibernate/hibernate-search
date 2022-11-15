/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.checkstyle.checks.regexp;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.puppycrawl.tools.checkstyle.api.FileContents;
import com.puppycrawl.tools.checkstyle.api.FileText;

/**
 * @author Davide D'Alto
 */
class StringSuppressorTest {

	@Test
	void testOutsideString() throws Exception {
		StringSuppressor suppressor = new StringSuppressor();
		suppressor.setCurrentContents( content( "  " ) );
		assertThat( suppressor.shouldSuppress( 1, 0, 0, 1 ) ).isFalse();
	}

	@Test
	void testInsideString() throws Exception {
		StringSuppressor suppressor = new StringSuppressor();
		suppressor.setCurrentContents( content( "\"           \"" ) );
		assertThat( suppressor.shouldSuppress( 1, 4, 0, 6 ) ).isTrue();
	}

	@Test
	void testInsideStringWithText() throws Exception {
		StringSuppressor suppressor = new StringSuppressor();
		suppressor.setCurrentContents( content( "\"text   text\"" ) );
		assertThat( suppressor.shouldSuppress( 1, 4, 0, 11 ) ).isTrue();
	}

	@Test
	void testInsideStringWithCode() throws Exception {
		StringSuppressor suppressor = new StringSuppressor();
		suppressor.setCurrentContents( content( "System.out.println(\"text   text\");" ) );
		assertThat( suppressor.shouldSuppress( 1, 25, 0, 27 ) ).isTrue();
	}

	@Test
	void testOutsideStringWithCode() throws Exception {
		StringSuppressor suppressor = new StringSuppressor();
		suppressor.setCurrentContents( content( "System.out.println   (\"text text\");" ) );
		assertThat( suppressor.shouldSuppress( 1, 18, 0, 20 ) ).isFalse();
	}

	private FileContents content(String string) {
		FileText fileText = new FileText( new File( "" ), Arrays.asList( string ) );
		return new FileContents( fileText );
	}
}
