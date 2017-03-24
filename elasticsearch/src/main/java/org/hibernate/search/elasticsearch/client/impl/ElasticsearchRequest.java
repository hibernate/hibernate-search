/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.client.impl;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.util.StringHelper;

import com.google.gson.JsonObject;

/**
 * @author Yoann Rodiere
 */
public final class ElasticsearchRequest {

	public static Builder put() {
		return new Builder( "PUT" );
	}

	public static Builder get() {
		return new Builder( "GET" );
	}

	public static Builder post() {
		return new Builder( "POST" );
	}

	public static Builder delete() {
		return new Builder( "DELETE" );
	}

	public static Builder head() {
		return new Builder( "HEAD" );
	}

	private final String method;
	private final String path;
	private final Map<String, String> parameters;
	private final List<JsonObject> bodyParts;

	private ElasticsearchRequest(Builder builder) {
		this.method = builder.method;
		this.path = builder.pathBuilder.toString();
		this.parameters = builder.parameters == null ? Collections.emptyMap() : Collections.unmodifiableMap( builder.parameters );
		this.bodyParts = builder.bodyParts == null ? Collections.emptyList() : Collections.unmodifiableList( builder.bodyParts );
	}

	public String getMethod() {
		return method;
	}

	public String getPath() {
		return path;
	}

	public Map<String, String> getParameters() {
		return parameters;
	}

	public List<JsonObject> getBodyParts() {
		return bodyParts;
	}

	public static final class Builder {
		private static final String PATH_SEPARATOR = "/";

		private final String method;
		private final StringBuilder pathBuilder = new StringBuilder( PATH_SEPARATOR );

		private Map<String, String> parameters;
		private List<JsonObject> bodyParts;

		private Builder(String method) {
			super();
			this.method = method;
		}

		public Builder pathComponent(String pathComponent) {
			try {
				String encodedPathComponent = URLEncoder.encode( pathComponent, StandardCharsets.UTF_8.name() );
				pathBuilder.append( encodedPathComponent ).append( PATH_SEPARATOR );
			}
			catch (UnsupportedEncodingException e) {
				throw new AssertionFailure( "Unexpected error retrieving the UTF-8 charset", e );
			}
			return this;
		}

		public Builder multiValuedPathComponent(Iterable<String> indexNames) {
			return pathComponent( StringHelper.join( indexNames, "," ) );
		}

		public Builder param(String name, String value) {
			if ( parameters == null ) {
				parameters = new LinkedHashMap<>();
			}
			parameters.put( name, value );
			return this;
		}

		public Builder param(String name, int value) {
			return param( name, String.valueOf( value ) );
		}

		public Builder param(String name, boolean value) {
			return param( name, String.valueOf( value ) );
		}

		public Builder body(JsonObject object) {
			if ( bodyParts == null ) {
				bodyParts = new ArrayList<>();
			}
			bodyParts.add( object );
			return this;
		}

		public ElasticsearchRequest build() {
			return new ElasticsearchRequest( this );
		}
	}

}
