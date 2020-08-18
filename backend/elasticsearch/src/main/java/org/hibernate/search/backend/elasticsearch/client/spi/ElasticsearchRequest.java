/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.client.spi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.engine.search.timeout.spi.TimeoutManager;

import com.google.gson.JsonObject;


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

	public static Builder builder(String method) {
		return new Builder( method );
	}

	private final String method;
	private final String path;
	private final Map<String, String> parameters;
	private final List<JsonObject> bodyParts;
	private final TimeoutManager timeoutManager;

	private ElasticsearchRequest(Builder builder) {
		this.method = builder.method;
		this.path = builder.pathBuilder.toString();
		this.parameters = builder.parameters == null ? Collections.emptyMap() : Collections.unmodifiableMap( builder.parameters );
		this.bodyParts = builder.bodyParts == null ? Collections.emptyList() : Collections.unmodifiableList( builder.bodyParts );
		this.timeoutManager = builder.timeoutManager;
	}

	public String method() {
		return method;
	}

	public String path() {
		return path;
	}

	public Map<String, String> parameters() {
		return parameters;
	}

	public List<JsonObject> bodyParts() {
		return bodyParts;
	}

	public TimeoutManager timeoutManager() {
		return timeoutManager;
	}

	@Override
	public String toString() {
		return new StringJoiner( ", ", ElasticsearchRequest.class.getSimpleName() + "[", "]" )
				.add( "method='" + method + "'" )
				.add( "path='" + path + "'" )
				.add( "parameters=" + parameters )
				.add( "bodyParts=" + bodyParts )
				.add( "timeoutManager=" + timeoutManager )
				.toString();
	}

	public static final class Builder {
		private static final char PATH_SEPARATOR = '/';

		private final String method;
		private final StringBuilder pathBuilder = new StringBuilder( 20 );

		private Map<String, String> parameters;
		private List<JsonObject> bodyParts;
		private TimeoutManager timeoutManager;

		private Builder(String method) {
			super();
			this.method = method;
		}

		public Builder wholeEncodedPath(String path) {
			pathBuilder.setLength( 0 );
			pathBuilder.append( path );
			return this;
		}

		public Builder pathComponent(URLEncodedString pathComponent) {
			pathBuilder.append( PATH_SEPARATOR ).append( pathComponent.encoded );
			return this;
		}

		public Builder multiValuedPathComponent(Iterable<URLEncodedString> indexNames) {
			boolean first = true;
			for ( URLEncodedString name : indexNames ) {
				if ( !first ) {
					pathBuilder.append( ',' );
				}
				else {
					pathBuilder.append( PATH_SEPARATOR );
					first = false;
				}
				pathBuilder.append( name.encoded );
			}
			return this;
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

		public Builder param(String name, long value) {
			return param( name, String.valueOf( value ) );
		}

		public Builder param(String name, boolean value) {
			return param( name, String.valueOf( value ) );
		}

		public Builder multiValuedParam(String name, Collection<String> values) {
			return param( name, String.join( ",", values ) );
		}

		public Builder body(JsonObject object) {
			if ( bodyParts == null ) {
				bodyParts = new ArrayList<>();
			}
			bodyParts.add( object );
			return this;
		}

		public Builder timeout(TimeoutManager timeoutManager) {
			this.timeoutManager = timeoutManager;
			return this;
		}

		public ElasticsearchRequest build() {
			return new ElasticsearchRequest( this );
		}
	}

}
