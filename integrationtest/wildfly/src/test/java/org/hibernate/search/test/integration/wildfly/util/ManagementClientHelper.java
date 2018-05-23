/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.integration.wildfly.util;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

import org.hibernate.search.exception.AssertionFailure;

import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.dmr.ModelNode;

public class ManagementClientHelper {

	private final ManagementClient managementClient;

	public ManagementClientHelper(ManagementClient managementClient) {
		this.managementClient = managementClient;
	}

	public Stream<String> getAllLogs() {
		return getLogFileNames().flatMap( this::getLogsFromFile );
	}

	public Stream<String> getLogFileNames() {
		ModelNode address = PathAddress.pathAddress( "subsystem", "logging" ).toModelNode();
		ModelNode op = Operations.createOperation( "list-log-files", address );
		ModelNode outcome;
		try {
			outcome = execute( op );
		}
		catch (IOException | RuntimeException e) {
			throw new AssertionFailure( "Could not list log files", e );
		}
		List<ModelNode> resultList = outcome.get( "result" ).asList();
		return resultList.stream()
				.map( resultItem -> resultItem.get( "file-name" ).asString() );
	}

	public Stream<String> getLogsFromFile(String filename) {
		ModelNode address = PathAddress.pathAddress( "subsystem", "logging" ).toModelNode();
		ModelNode op = Operations.createOperation( "read-log-file", address );
		op.get( "name" ).set( filename );
		op.get( "lines" ).set( -1 );
		ModelNode outcome;
		try {
			outcome = execute( op );
		}
		catch (IOException | RuntimeException e) {
			throw new AssertionFailure( "Could not read log file " + filename, e );
		}
		List<ModelNode> resultList = outcome.get( "result" ).asList();
		return resultList.stream().map( ModelNode::asString );
	}

	private ModelNode execute(ModelNode op) throws IOException {
		ModelNode outcome = managementClient.getControllerClient().execute( op );
		if ( !Operations.isSuccessfulOutcome( outcome ) ) {
			throw new IllegalStateException( "Unsuccessful API call outcome: " + outcome );
		}
		return outcome;
	}
}
