/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.client.common.cfg;

import java.util.Collections;
import java.util.List;

/**
 * Common configuration properties for the Elasticsearch backend's rest client.
 * <p>
 * Constants in this class are to be appended to a prefix to form a property key;
 * see {@link org.hibernate.search.engine.cfg.BackendSettings} for details.
 *
 * @author Gunnar Morling
 */
public final class ElasticsearchBackendClientCommonSettings {

	private ElasticsearchBackendClientCommonSettings() {
	}

	/**
	 * The hostname and ports of the Elasticsearch servers to connect to.
	 * <p>
	 * Expects a String representing a hostname and port such as {@code localhost} or {@code es.mycompany.com:4400},
	 * or a String containing multiple such hostname-and-port strings separated by commas,
	 * or a {@code Collection<String>} containing such hostname-and-port strings.
	 * <p>
	 * Multiple servers may be specified for load-balancing: requests will be assigned to each host in turns.
	 * <p>
	 * Setting this property at the same time as {@link #URIS} will lead to an exception being thrown on startup.
	 * <p>
	 * Defaults to {@link Defaults#HOSTS}.
	 */
	public static final String HOSTS = "hosts";

	/**
	 * The protocol to use when connecting to the Elasticsearch servers.
	 * <p>
	 * Expects a String: either {@code http} or {@code https}.
	 * <p>
	 * Setting this property at the same time as {@link #URIS} will lead to an exception being thrown on startup.
	 * <p>
	 * Defaults to {@link Defaults#PROTOCOL}.
	 */
	public static final String PROTOCOL = "protocol";

	/**
	 * The protocol, hostname and ports of the Elasticsearch servers to connect to.
	 * <p>
	 * Expects either a String representing an URI such as {@code http://localhost}
	 * or {@code https://es.mycompany.com:4400},
	 * or a String containing multiple such URIs separated by commas,
	 * or a {@code Collection<String>} containing such URIs.
	 * <p>
	 * All the URIs must specify the same protocol.
	 * <p>
	 * Setting this property at the same time as {@link #HOSTS} or {@link #PROTOCOL} will lead to an exception being thrown on startup.
	 * <p>
	 * Defaults to {@code http://localhost:9200}, unless {@link #HOSTS} or {@link #PROTOCOL} are set, in which case they take precedence.
	 */
	public static final String URIS = "uris";

	/**
	 * Property for specifying the path prefix prepended to the request end point.
	 * Use the path prefix if your Elasticsearch instance is located at a specific context path.
	 * <p>
	 * Defaults to {@link Defaults#PATH_PREFIX}.
	 */
	public static final String PATH_PREFIX = "path_prefix";

	/**
	 * The username to send when connecting to the Elasticsearch servers (HTTP authentication).
	 * <p>
	 * Expects a String.
	 * <p>
	 * Defaults to no username (anonymous access).
	 */
	public static final String USERNAME = "username";

	/**
	 * The password to send when connecting to the Elasticsearch servers (HTTP authentication).
	 * <p>
	 * Expects a String.
	 * <p>
	 * Defaults to no username (anonymous access).
	 */
	public static final String PASSWORD = "password";

	/**
	 * Default values for the different settings if no values are given.
	 */
	public static final class Defaults {

		private Defaults() {
		}

		public static final List<String> HOSTS = Collections.singletonList( "localhost:9200" );
		public static final String PROTOCOL = "http";
		public static final String PATH_PREFIX = "";
	}
}
