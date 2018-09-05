/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engineperformance.elasticsearch.setuputilities;

public class ConnectionInfo {

	private final String host;
	private final String username;
	private final String password;
	private final String awsAccessKey;
	private final String awsSecretKey;
	private final String awsRegion;

	public ConnectionInfo(String host, String username, String password,
			String awsAccessKey, String awsSecretKey, String awsRegion) {
		super();
		this.host = host;
		this.username = username;
		this.password = password;
		this.awsAccessKey = awsAccessKey;
		this.awsSecretKey = awsSecretKey;
		this.awsRegion = awsRegion;
	}

	public String getHost() {
		return host;
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	public String getAwsAccessKey() {
		return awsAccessKey;
	}

	public String getAwsSecretKey() {
		return awsSecretKey;
	}

	public String getAwsRegion() {
		return awsRegion;
	}

}
