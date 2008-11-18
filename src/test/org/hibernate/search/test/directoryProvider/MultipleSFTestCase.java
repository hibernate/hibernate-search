//$Id$
package org.hibernate.search.test.directoryProvider;

import java.io.InputStream;

import junit.framework.TestCase;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;

/**
 * Build multiple session factories from the same set of classes
 * The configuration can be altered overriding {@link #configure}.
 *
 * @author Emmanuel Bernard
 */
public abstract class MultipleSFTestCase extends TestCase {

	private static SessionFactory[] sessionFactories;
	private static AnnotationConfiguration[] cfgs;
	private static Dialect dialect;
	private static Class lastTestClass;

	protected abstract int getSFNbrs();

	protected void buildSessionFactories(Class[] classes, String[] packages, String[] xmlFiles) throws Exception {
		if ( sessionFactories == null ) {
			sessionFactories = new SessionFactory[getSFNbrs()];
		}
		if ( cfgs == null ) {
			cfgs = new AnnotationConfiguration[getSFNbrs()];
		}
		for ( SessionFactory sf : sessionFactories ) {
			if ( sf != null ) {
				sf.close();
			}
		}
		for ( int sfIndex = 0; sfIndex < getSFNbrs(); sfIndex++ ) {
			cfgs[sfIndex] = new AnnotationConfiguration();
		}
		configure( cfgs );
		for ( int sfIndex = 0; sfIndex < getSFNbrs(); sfIndex++ ) {
			try {
				if ( recreateSchema() ) {
					cfgs[sfIndex].setProperty( Environment.HBM2DDL_AUTO, "create-drop" );
				}
				for ( String aPackage : packages ) {
					cfgs[sfIndex].addPackage( aPackage );
				}
				for ( Class aClass : classes ) {
					cfgs[sfIndex].addAnnotatedClass( aClass );
				}
				for ( String xmlFile : xmlFiles ) {
					InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream( xmlFile );
					cfgs[sfIndex].addInputStream( is );
				}
				setDialect( Dialect.getDialect() );
				sessionFactories[sfIndex] = cfgs[sfIndex].buildSessionFactory( /*new TestInterceptor()*/ );
			}
			catch ( Exception e ) {
				e.printStackTrace();
				throw e;
			}
		}
	}

	protected void setUp() throws Exception {
		if ( sessionFactories == null || sessionFactories[0] == null || lastTestClass != getClass() ) {
			buildSessionFactories( getMappings(), getAnnotatedPackages(), getXmlFiles() );
			lastTestClass = getClass();
		}
	}

	protected void tearDown() throws Exception {
		for ( SessionFactory sf : getSessionFactories() ) {
			sf.close();
		}
	}

	protected abstract Class[] getMappings();

	protected String[] getAnnotatedPackages() {
		return new String[] { };
	}

	protected String[] getXmlFiles() {
		return new String[] { };
	}

	private void setDialect(Dialect dialect) {
		MultipleSFTestCase.dialect = dialect;
	}

	protected Dialect getDialect() {
		return dialect;
	}

	protected abstract void configure(Configuration[] cfg);

	protected boolean recreateSchema() {
		return true;
	}

	public static SessionFactory[] getSessionFactories() {
		return sessionFactories;
	}
}
