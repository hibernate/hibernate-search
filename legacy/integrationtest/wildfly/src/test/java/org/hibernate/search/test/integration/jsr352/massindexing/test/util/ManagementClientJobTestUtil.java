/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.integration.jsr352.massindexing.test.util;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;
import javax.batch.runtime.BatchStatus;

import org.hibernate.search.jsr352.logging.impl.Log;
import org.hibernate.search.jsr352.massindexing.MassIndexingJob;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.dmr.ModelNode;

public class ManagementClientJobTestUtil {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final DateTimeFormatter WILDFLY_INSTANT_FORMAT = new DateTimeFormatterBuilder()
			.parseCaseInsensitive()
			.append( DateTimeFormatter.ISO_LOCAL_DATE_TIME )
			.appendOffset( "+HHMM", "+0000" )
			.toFormatter( Locale.ROOT );

	private static final int THREAD_SLEEP = 1000;

	private final ManagementClient managementClient;
	private final PathAddress jberetSubsystemAddress;
	private final String xmlJobName;

	public ManagementClientJobTestUtil(ManagementClient managementClient,
			PathAddress jberetSubsystemAddress, String xmlJobName) {
		this.managementClient = managementClient;
		this.jberetSubsystemAddress = jberetSubsystemAddress;
		this.xmlJobName = xmlJobName;
	}

	public long start(Properties properties) throws IOException {
		ModelNode op = Operations.createOperation( "start-job", jberetSubsystemAddress.toModelNode() );
		op.get( "job-xml-name" ).set( MassIndexingJob.NAME );
		if ( properties != null ) {
			op.get( "properties" ).set( toModelNode( properties ) );
		}
		final ModelNode outcome = managementClient.getControllerClient().execute( op );
		if ( !Operations.isSuccessfulOutcome( outcome ) ) {
			throw new IllegalStateException( "Could not start job: " + outcome );
		}
		return outcome.get( "result" ).asLong();
	}

	public long restart(long executionId, Properties properties) throws IOException {
		ModelNode op = Operations.createOperation( "restart-job", jberetSubsystemAddress.toModelNode() );
		op.get( "execution-id" ).set( executionId );
		if ( properties != null ) {
			op.get( "properties" ).set( toModelNode( properties ) );
		}
		final ModelNode outcome = managementClient.getControllerClient().execute( op );
		if ( !Operations.isSuccessfulOutcome( outcome ) ) {
			throw new IllegalStateException( "Could not restart job: " + outcome );
		}
		return outcome.get( "result" ).asLong();
	}

	public BatchStatus waitForTermination(long executionId, int timeoutInMs)
			throws InterruptedException, IOException {
		long endTime = System.currentTimeMillis() + timeoutInMs;

		BatchStatus executionStatus;

		executionStatus = getExecutionStatus( executionId );
		while ( !executionStatus.equals( BatchStatus.COMPLETED )
				&& !executionStatus.equals( BatchStatus.STOPPED )
				&& !executionStatus.equals( BatchStatus.FAILED )
				&& System.currentTimeMillis() < endTime ) {
			log.infof(
					"Job execution (id=%d) has status %s. Thread sleeps %d ms...",
					executionId,
					executionStatus,
					THREAD_SLEEP
			);
			Thread.sleep( THREAD_SLEEP );
			executionStatus = getExecutionStatus( executionId );
		}

		return executionStatus;
	}

	public Optional<Long> getLastExecutionId() throws IOException {
		ModelNode address = PathAddress.pathAddress( jberetSubsystemAddress )
				.append( "job", xmlJobName )
				.append( "execution" )
				.toModelNode();
		ModelNode op = Operations.createOperation( "read-resource", address );
		op.get( "include-runtime" ).set( true );
		ModelNode outcome = managementClient.getControllerClient().execute( op );
		if ( !Operations.isSuccessfulOutcome( outcome ) ) {
			throw new IllegalStateException( "Could not restart job: " + outcome );
		}
		List<ModelNode> resultList = outcome.get( "result" ).asList();
		return resultList.stream()
				/*
				 * Each result item is an object containing several attributes,
				 * among which the "result" which is the execution we are looking for.
				 */
				.map( resultItem -> resultItem.get( "result" ) )
				// Executions are not ordered, so we must use a comparator to find the last one
				.max( Comparator.comparing( execution -> {
					String instantString = execution.get( "create-time" ).asString();
					return WILDFLY_INSTANT_FORMAT.parse( instantString, Instant::from );
				} ) )
				.map( execution -> execution.get( "instance-id" ).asLong() );
	}

	private BatchStatus getExecutionStatus(long executionId) throws IOException {
		ModelNode address = PathAddress.pathAddress( jberetSubsystemAddress )
				.append( "job", xmlJobName )
				.append( "execution", String.valueOf( executionId ) )
				.toModelNode();
		ModelNode op = Operations.createOperation( "read-resource", address );
		op.get( "recursive" ).set( true );
		op.get( "include-runtime" ).set( true );
		final ModelNode outcome = managementClient.getControllerClient().execute( op );
		if ( !Operations.isSuccessfulOutcome( outcome ) ) {
			throw new IllegalStateException( "Could not restart job: " + outcome );
		}
		return BatchStatus.valueOf( outcome.get( "result" ).get( "batch-status" ).asString() );
	}

	private static ModelNode toModelNode(Properties properties) {
		ModelNode modelNode = new ModelNode();
		for ( String key : properties.stringPropertyNames() ) {
			modelNode.add( key, properties.getProperty( key ) );
		}
		return modelNode;
	}
}
