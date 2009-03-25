//$Id$
package org.hibernate.search.test;

import java.io.InputStream;

import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;

/**
 * Originally a copy from Hibernate Annotations.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public abstract class HANTestCase extends TestCase {

	public HANTestCase() {
		super();
	}

	public HANTestCase(String x) {
		super( x );
	}

	protected void buildSessionFactory(Class[] classes, String[] packages, String[] xmlFiles) throws Exception {
		if ( getSessions() != null ) {
			getSessions().close();
		}
		try {
			setCfg( new AnnotationConfiguration() );
			configure( cfg );
			if ( recreateSchema() ) {
				cfg.setProperty( Environment.HBM2DDL_AUTO, "create-drop" );
			}
			for ( String aPackage : packages ) {
				( ( AnnotationConfiguration ) getCfg() ).addPackage( aPackage );
			}
			for ( Class aClass : classes ) {
				( ( AnnotationConfiguration ) getCfg() ).addAnnotatedClass( aClass );
			}
			for ( String xmlFile : xmlFiles ) {
				InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream( xmlFile );
				getCfg().addInputStream( is );
			}
			setDialect( Dialect.getDialect() );
			setSessions( getCfg().buildSessionFactory( /*new TestInterceptor()*/ ) );
		}
		catch ( Exception e ) {
			e.printStackTrace();
			throw e;
		}
	}

	protected void setUp() throws Exception {
		if ( getSessions() == null || getSessions().isClosed() || lastTestClass != getClass() ) {
			buildSessionFactory( getMappings(), getAnnotatedPackages(), getXmlFiles() );
			lastTestClass = getClass();
		}
	}

	protected abstract Class[] getMappings();

	protected String[] getAnnotatedPackages() {
		return new String[] { };
	}
}
