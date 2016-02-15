package org.hibernate.search.genericjpa.test.db.events.jpa;

import java.sql.SQLException;

import org.hibernate.search.genericjpa.db.events.triggers.HSQLDBTriggerSQLStringSource;

import org.junit.Before;
import org.junit.Test;

/**
 * Created by Martin on 22.07.2015.
 */
public class HSQLDBTriggerIntegrationTest extends DatabaseIntegrationTest {

	@Before
	public void setup() throws SQLException {
		this.setup( "EclipseLink_HSQLDB", new HSQLDBTriggerSQLStringSource() );
		this.setupTriggers( new HSQLDBTriggerSQLStringSource() );
	}

	@Test
	public void test() throws InterruptedException {
		this.testUpdateIntegration();
	}

}
