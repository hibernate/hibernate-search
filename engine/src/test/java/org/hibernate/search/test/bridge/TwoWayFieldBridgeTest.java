/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.bridge;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.junit.Test;

import org.hibernate.search.bridge.TwoWayFieldBridge;
import org.hibernate.search.bridge.builtin.NumericFieldBridge;
import org.hibernate.search.testsupport.TestForIssue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Hardy Ferentschik
 */
@TestForIssue(jiraKey = "HSEARCH-1744")
public class TwoWayFieldBridgeTest {

	@Test
	public void testTwoWayFieldBridgeCanHandleNullInObjectToString() throws Exception {
		List<Class<?>> classes = getClasses( "org.hibernate.search.bridge.builtin" );
		assertTrue( "Guarding against a package refactoring", classes.size() > 0 );

		for ( Class<?> clazz : classes ) {
			// not interested in abstract classes
			if ( Modifier.isAbstract( clazz.getModifiers() ) ) {
				continue;
			}
			// neither in non TwoWayFieldBridge
			if ( !TwoWayFieldBridge.class.isAssignableFrom( clazz ) ) {
				continue;
			}

			// the NumericFieldBridge is an emum (go figure) - we need special massaging here
			if ( Enum.class.isAssignableFrom( clazz ) ) {
				assertTrue( "Unexpected enum class" + clazz, NumericFieldBridge.class.isAssignableFrom( clazz ) );
				for ( Object o : clazz.getEnclosingClass().getEnumConstants() ) {
					TwoWayFieldBridge bridge = (TwoWayFieldBridge) o;
					assertEquals(
							"All TwoWayFieldBridgeTest should return 'null' for 'null' passed to 'objectToString",
							null,
							bridge.objectToString( null )
					);
				}
			}
			else {
				TwoWayFieldBridge bridge = (TwoWayFieldBridge) clazz.newInstance();
				assertEquals(
						"All TwoWayFieldBridgeTest should return 'null' for 'null' passed to 'objectToString",
						null,
						bridge.objectToString( null )
				);
			}
		}
	}

	private List<Class<?>> getClasses(String packageName) throws ClassNotFoundException, IOException {
		String path = packageName.replace( '.', '/' );
		Enumeration<URL> resources = TwoWayFieldBridgeTest.class.getClassLoader().getResources( path );
		List<File> directories = new ArrayList<>();
		while ( resources.hasMoreElements() ) {
			URL resource = resources.nextElement();
			directories.add( new File( resource.getFile() ) );
		}
		ArrayList<Class<?>> classes = new ArrayList<>();
		for ( File directory : directories ) {
			for ( String classFileName : directory.list( new ClassFilenameFilter() ) ) {
				classes.add(
						Class.forName( packageName + '.' + classFileName.substring( 0, classFileName.length() - 6 ) )
				);
			}
		}
		return classes;
	}

	public static class ClassFilenameFilter implements FilenameFilter {

		@Override
		public boolean accept(File dir, String name) {
			return name.endsWith( ".class" );
		}
	}
}
