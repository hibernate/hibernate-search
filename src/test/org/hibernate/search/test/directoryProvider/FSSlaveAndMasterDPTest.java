//$Id$
package org.hibernate.search.test.directoryProvider;

import java.io.File;
import java.util.Date;
import java.util.List;

import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.queryParser.QueryParser;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.util.FileHelper;

/**
 * @author Emmanuel Bernard
 */
public class FSSlaveAndMasterDPTest extends MultipleSFTestCase {
	
	private static File root;
	static {
		String buildDir = System.getProperty("build.dir");
		if (buildDir == null) {
			buildDir = ".";
		}
		root = new File(buildDir, "lucenedirs");
	}

	@SuppressWarnings( { "PointlessArithmeticExpression" } )
	public void testProperCopy() throws Exception {
		Session s1 = getSessionFactories()[0].openSession( );
		SnowStorm sn = new SnowStorm();
		sn.setDate( new Date() );
		sn.setLocation( "Dallas, TX, USA");

		FullTextSession fts2 = Search.getFullTextSession( getSessionFactories()[1].openSession( ) );
		QueryParser parser = new QueryParser("id", new StopAnalyzer() );
		List result = fts2.createFullTextQuery( parser.parse( "location:texas" ) ).list();
		assertEquals( "No copy yet, fresh index expected", 0, result.size() );

		s1.persist( sn );
		s1.flush(); //we don' commit so we need to flush manually

		fts2.close();
		s1.close();

		int waitPeroid = 2 * 1 * 1000 + 10; //wait a bit more than 2 refresh (one master / one slave)
		Thread.sleep( waitPeroid );

		//temp test original
		fts2 = Search.getFullTextSession( getSessionFactories()[0].openSession( ) );
		result = fts2.createFullTextQuery( parser.parse( "location:dallas" ) ).list();
		assertEquals( "Original should get one", 1, result.size() );

		fts2 = Search.getFullTextSession( getSessionFactories()[1].openSession( ) );
		result = fts2.createFullTextQuery( parser.parse( "location:dallas" ) ).list();
		assertEquals("First copy did not work out", 1, result.size() );

		s1 = getSessionFactories()[0].openSession( );
		sn = new SnowStorm();
		sn.setDate( new Date() );
		sn.setLocation( "Chennai, India");

		s1.persist( sn );
		s1.flush(); //we don' commit so we need to flush manually

		fts2.close();
		s1.close();

		Thread.sleep( waitPeroid ); //wait a bit more than 2 refresh (one master / one slave)

		fts2 = Search.getFullTextSession( getSessionFactories()[1].openSession( ) );
		result = fts2.createFullTextQuery( parser.parse( "location:chennai" ) ).list();
		assertEquals("Second copy did not work out", 1, result.size() );

		s1 = getSessionFactories()[0].openSession( );
		sn = new SnowStorm();
		sn.setDate( new Date() );
		sn.setLocation( "Melbourne, Australia");

		s1.persist( sn );
		s1.flush(); //we don' commit so we need to flush manually

		fts2.close();
		s1.close();

		Thread.sleep( waitPeroid ); //wait a bit more than 2 refresh (one master / one slave)

		fts2 = Search.getFullTextSession( getSessionFactories()[1].openSession( ) );
		result = fts2.createFullTextQuery( parser.parse( "location:melbourne" ) ).list();
		assertEquals("Third copy did not work out", 1, result.size() );

		fts2.close();
		//run the searchfactory.close() operations
		for ( SessionFactory sf : getSessionFactories() ) {
			sf.close();
		}
	}

	protected void setUp() throws Exception {
		root.mkdir();

		File master = new File(root, "master/main");
		master.mkdirs();
		master = new File(root, "master/copy");
		master.mkdirs();

		File slave = new File(root, "slave");
		slave.mkdir();
		                                                                            
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		FileHelper.delete( root );
	}

	protected int getSFNbrs() {
		return 2;
	}

	@SuppressWarnings("unchecked")
	protected Class[] getMappings() {
		return new Class[] {
				SnowStorm.class
		};
	}

	protected void configure(Configuration[] cfg) {
		//master
		cfg[0].setProperty( "hibernate.search.default.sourceBase",  root.getAbsolutePath() + "/master/copy");
		cfg[0].setProperty( "hibernate.search.default.indexBase", root.getAbsolutePath() + "/master/main");
		cfg[0].setProperty( "hibernate.search.default.refresh", "1"); //every minute
		cfg[0].setProperty( "hibernate.search.default.directory_provider", "org.hibernate.search.store.FSMasterDirectoryProvider");

		//slave(s)
		cfg[1].setProperty( "hibernate.search.default.sourceBase", root.getAbsolutePath() + "/master/copy");
		cfg[1].setProperty( "hibernate.search.default.indexBase", root.getAbsolutePath() + "/slave");
		cfg[1].setProperty( "hibernate.search.default.refresh", "1"); //every minute
		cfg[1].setProperty( "hibernate.search.default.directory_provider", "org.hibernate.search.store.FSSlaveDirectoryProvider");
	}
}
