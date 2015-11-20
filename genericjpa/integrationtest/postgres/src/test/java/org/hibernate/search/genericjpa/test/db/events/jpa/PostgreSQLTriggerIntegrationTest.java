/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.genericjpa.test.db.events.jpa;

import java.sql.SQLException;

import org.hibernate.search.genericjpa.db.events.triggers.PostgreSQLTriggerSQLStringSource;

import org.junit.Before;
import org.junit.Test;

/**
 * Created by Martin on 02.07.2015.
 */
public class PostgreSQLTriggerIntegrationTest extends DatabaseIntegrationTest {

	@Before
	public void setup() throws SQLException {
		this.setup( "EclipseLink_PostgreSQL", new PostgreSQLTriggerSQLStringSource() );
		this.setupTriggers( new PostgreSQLTriggerSQLStringSource() );
	}

	@Test
	public void test() throws InterruptedException {
		this.testUpdateIntegration();
	}

}
