/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.integration.jsr352.massindexing;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.text.ParseException;

import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;

import org.hibernate.search.jsr352.massindexing.MassIndexingJob;
import org.hibernate.search.jsr352.test.util.JobTestUtil;
import org.hibernate.search.test.integration.jsr352.massindexing.test.common.Message;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test the behavior when there are multiple entity manager factories (persistence units),
 * and they haven't been registered as CDI beans.
 *
 * @author Mincong Huang
 */
@RunWith(Arquillian.class)
public class MultipleEntityManagerFactoriesNotRegisteredAsBeansIT {

	private static final String PERSISTENCE_UNIT_NAME = "h2";

	private static final int JOB_TIMEOUT_MS = 40_000;

	@Deployment
	public static WebArchive createDeployment() {
		WebArchive war = ShrinkWrap
				.create( WebArchive.class, MultipleEntityManagerFactoriesNotRegisteredAsBeansIT.class.getSimpleName() + ".war" )
				.addAsResource( "jsr352/persistence_multiple.xml", "META-INF/persistence.xml" )
				.addAsWebInfResource( "jboss-deployment-structure-jsr352.xml", "/jboss-deployment-structure.xml" )
				.addAsWebInfResource( EmptyAsset.INSTANCE, "beans.xml" )
				.addPackage( JobTestUtil.class.getPackage() )
				.addPackage( Message.class.getPackage() );
		return war;
	}

	@Test
	public void testJob() throws InterruptedException, IOException, ParseException {
		JobOperator jobOperator = BatchRuntime.getJobOperator();

		long execId1 = jobOperator.start(
				MassIndexingJob.NAME,
				MassIndexingJob.parameters()
						.forEntity( Message.class )
						.entityManagerFactoryReference( PERSISTENCE_UNIT_NAME )
						.build()
				);
		JobExecution jobExec1 = jobOperator.getJobExecution( execId1 );
		JobTestUtil.waitForTermination( jobOperator, jobExec1, JOB_TIMEOUT_MS );

		/*
		 * We expect failure, because we can only retrieve the default PU by default
		 * (unless a non-default scope is used, but that requires user configuration).
		 */
		assertEquals( BatchStatus.FAILED, jobExec1.getBatchStatus() );
	}
}
