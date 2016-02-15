/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.genericjpa.test.db.events;

import java.util.Arrays;
import java.util.HashSet;

import org.hibernate.search.genericjpa.db.events.impl.AnnotationEventModelParser;
import org.hibernate.search.genericjpa.db.events.impl.EventModelInfo;
import org.hibernate.search.genericjpa.db.events.impl.EventModelParser;
import org.hibernate.search.genericjpa.db.EventType;
import org.hibernate.search.genericjpa.db.events.triggers.HSQLDBTriggerSQLStringSource;
import org.hibernate.search.genericjpa.db.events.triggers.MySQLTriggerSQLStringSource;
import org.hibernate.search.genericjpa.db.events.triggers.PostgreSQLTriggerSQLStringSource;
import org.hibernate.search.genericjpa.db.events.triggers.TriggerSQLStringSource;
import org.hibernate.search.genericjpa.test.db.entities.Place;

import org.junit.Test;

/**
 * @author Martin
 */
public class TriggerSQLStringSourceTest {

	@Test
	public void testMySQLSTringSource() {
		this.test( new MySQLTriggerSQLStringSource() );
	}

	@Test
	public void testPostgreSQLTriggerSQLStringSource() {
		this.test( new PostgreSQLTriggerSQLStringSource() );
	}

	@Test
	public void testHSQLDBTriggerSQLStringSource() {
		this.test( new HSQLDBTriggerSQLStringSource() );
	}

	private void test(TriggerSQLStringSource triggerSource) {
		EventModelParser parser = new AnnotationEventModelParser();
		EventModelInfo info = parser.parse( new HashSet<>( Arrays.asList( Place.class ) ) ).get( 0 );
		System.out.println( "UNSETUP CODE: " + Arrays.asList( triggerSource.getUnSetupCode() ) );
		System.out.println( "SETUP CODE: " + Arrays.asList( triggerSource.getSetupCode() ) );
		for ( int eventType : EventType.values() ) {
			String[] updateTableCreationString = triggerSource.getUpdateTableCreationCode( info );
			String[] updateTableDropString = triggerSource.getUpdateTableDropCode( info );
			String[] triggerCreationString = triggerSource.getTriggerCreationCode( info, eventType );
			String[] triggerDropString = triggerSource.getTriggerDropCode( info, eventType );
			System.out.println( "CREATE TABLES: " + Arrays.asList( updateTableCreationString ) );
			System.out.println( "DROP TABLES: " + Arrays.asList( updateTableDropString ) );
			System.out.println( "CREATE TRIGGERS: " + Arrays.asList( triggerCreationString ) );
			System.out.println( "DROP TRIGGERS: " + Arrays.asList( triggerDropString ) );
		}
	}

}
