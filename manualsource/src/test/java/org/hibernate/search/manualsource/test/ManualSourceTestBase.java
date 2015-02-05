/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.manualsource.test;

import java.io.File;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.apache.lucene.analysis.core.StopAnalyzer;
import org.junit.After;
import org.junit.Before;

import org.hibernate.search.cfg.Environment;
import org.hibernate.search.manualsource.WorkLoadManager;
import org.hibernate.search.manualsource.impl.WorkLoadManagerImpl;
import org.hibernate.search.manualsource.source.IdExtractor;
import org.hibernate.search.testsupport.TestConstants;
import org.hibernate.search.util.impl.FileHelper;

/**
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public abstract class ManualSourceTestBase {
	private WorkLoadManager workLoadManager;
	private File baseIndexDir;

	@Before
	public void setUp() throws Exception {
		baseIndexDir = createBaseIndexDir();
		Properties properties = new Properties();
		properties.setProperty( "hibernate.search.lucene_version", TestConstants.getTargetLuceneVersion().toString() );
		properties.setProperty( "hibernate.search.default.directory_provider", "ram" );
		properties.setProperty( "hibernate.search.default.indexBase", getBaseIndexDir().getAbsolutePath() );
		properties.setProperty( Environment.ANALYZER_CLASS, StopAnalyzer.class.getName() );
		configure( properties );
		List<Class<?>> classes = Arrays.asList( getAnnotatedClasses() );
		workLoadManager = new WorkLoadManagerImpl( classes, new NameBasedIdExtractor( classes ), properties );
	}

	@After
	public void tearDown() throws Exception {
		workLoadManager.close();
		FileHelper.delete( getBaseIndexDir() );
	}

	public WorkLoadManager getWorkLoadManager() {
		return workLoadManager;
	}

	public File getBaseIndexDir() {
		return baseIndexDir;
	}


	private File createBaseIndexDir() {
		// Make sure no directory is ever reused across the test suite as Windows might not be able
		// to delete the files after usage. See also
		// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4715154
		String shortTestName = this.getClass().getSimpleName() + "-" + UUID.randomUUID().toString().substring( 0, 8 );

		// the constructor File(File, String) is broken too, see :
		// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5066567
		// So make sure to use File(String, String) in this case as TestConstants works with absolute paths!
		return new File( TestConstants.getIndexDirectory( this.getClass() ), shortTestName );
	}

	protected abstract Class<?>[] getAnnotatedClasses();

	protected void configure(Properties properties) {
	}

	/** Expects entities to have their id via the getId() method */
	public static class NameBasedIdExtractor implements IdExtractor {
		private Map<Class<?>, Method> idGetters;

		public NameBasedIdExtractor(List<Class<?>> classes) throws NoSuchMethodException {
			idGetters = new HashMap<>( classes.size() );
			for ( Class<?> clazz : classes ) {
				Method method = clazz.getMethod( "getId" );
				idGetters.put( clazz, method );
			}
		}

		@Override
		public Serializable getId(Object entity) {
			try {
				return (Serializable) idGetters.get( entity.getClass() ).invoke( entity );
			}
			catch (IllegalAccessException e) {
				throw new RuntimeException( "Oops", e );
			}
			catch (InvocationTargetException e) {
				throw new RuntimeException( "Oops", e );
			}
		}
	}
}
