/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.massindexing;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.text.ParseException;

import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;

import org.hibernate.search.jsr352.massindexing.BatchIndexingJob;
import org.hibernate.search.jsr352.massindexing.test.common.Message;
import org.hibernate.search.jsr352.test.util.JobTestUtil;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * This integration test (IT) aims to test the restartability of the job execution mass-indexer under Java EE
 * environment, with step partitioning (parallelism). We need to prove that the job restart from the checkpoint where it
 * was stopped, but not from the very beginning.
 *
 * @author Mincong Huang
 */
@RunWith(Arquillian.class)
public class MultiplePersistenceUnitsIT {

	private static final String PERSISTENCE_UNIT_NAME = "h2";

	private static final int JOB_TIMEOUT_MS = 40_000;

	@Deployment
	public static WebArchive createDeployment() {
		WebArchive war = ShrinkWrap
				.create( WebArchive.class, MultiplePersistenceUnitsIT.class.getSimpleName() + ".war" )
				.addAsResource( "META-INF/persistence_multiple.xml", "META-INF/persistence.xml" )
				.addAsResource( "META-INF/batch-jobs/make-deployment-as-batch-app.xml" ) // WFLY-7000
				.addAsWebInfResource( "jboss-deployment-structure.xml" )
				.addAsWebInfResource( EmptyAsset.INSTANCE, "beans.xml" )
				.addPackage( JobTestUtil.class.getPackage() )
				.addPackage( Message.class.getPackage() );
		return war;
	}

	@Test
	public void testJob() throws InterruptedException, IOException, ParseException {
		JobOperator jobOperator = BatchRuntime.getJobOperator();

		long execId1 = BatchIndexingJob.forEntity( Message.class )
				.entityManagerFactoryReference( PERSISTENCE_UNIT_NAME )
				.start();
		JobExecution jobExec1 = jobOperator.getJobExecution( execId1 );
		JobTestUtil.waitForTermination( jobOperator, jobExec1, JOB_TIMEOUT_MS );

		/*
		 * We expect failure, because we can only retrieve the default PU by default
		 * (unless a non-default scope is used, but that requires user configuration).
		 */
		assertEquals( BatchStatus.FAILED, jobExec1.getBatchStatus() );
	}
}
