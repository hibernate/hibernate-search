package org.hibernate.search.backend.elasticsearch.client.common.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

import org.hibernate.search.backend.elasticsearch.client.common.util.spi.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.json.impl.JsonHandlerBridge;
import org.hibernate.search.engine.common.timing.Deadline;
import org.hibernate.search.util.json.JsonHandler;
import org.hibernate.search.util.json.internal.spi.JsonTreeValidation;
import org.hibernate.search.util.json.internal.spi.JsonValidation;

import tools.jackson.databind.node.ObjectNode;

public final class ElasticsearchJsonRequest {

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
	private final List<ObjectNode> bodyParts;
	private final Deadline deadline;
	private final JsonValidation validation;

	private ElasticsearchJsonRequest(Builder builder) {
		this.method = builder.method;
		this.path = builder.pathBuilder.toString();
		this.parameters =
				builder.parameters == null ? Collections.emptyMap() : Collections.unmodifiableMap( builder.parameters );
		this.bodyParts =
				builder.bodyParts == null ? Collections.emptyList() : List.copyOf( builder.bodyParts );
		this.deadline = builder.deadline;
		this.validation = builder.validation;
	}

	private ElasticsearchJsonRequest(ElasticsearchJsonRequest original, JsonValidation validation) {
		this.method = original.method;
		this.path = original.path;
		this.parameters = original.parameters;
		this.bodyParts = original.bodyParts;
		this.deadline = original.deadline;
		this.validation = Objects.requireNonNull( validation );
	}

	/** Bind an immutable request view to the receiving client's JSON context. */
	public ElasticsearchJsonRequest withValidation(JsonValidation validation) {
		return new ElasticsearchJsonRequest( this, validation );
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

	/** Borrowed native parts; submitted trees must stay unchanged across retries. */
	public List<ObjectNode> bodyPartsJson() {
		return bodyParts;
	}

	public <T> List<T> bodyParts(JsonHandler<T> handler) {
		// Check handler/token even when there are no parts; an absent value needs no conversion.
		JsonHandlerBridge.exportObject( handler, null, "request body export", validation );
		List<T> result = new ArrayList<>( bodyParts.size() );
		for ( ObjectNode part : bodyParts ) {
			result.add( JsonHandlerBridge.exportObject( handler, part, "request body export", validation ) );
		}
		return Collections.unmodifiableList( result );
	}


	public Deadline deadline() {
		return deadline;
	}

	@Override
	public String toString() {
		return new StringJoiner( ", ", ElasticsearchJsonRequest.class.getSimpleName() + "[", "]" )
				.add( "method='" + method + "'" )
				.add( "path='" + path + "'" )
				.add( "parameters=" + parameters )
				.add( "bodyParts=" + bodyParts )
				.add( "deadline=" + deadline )
				.toString();
	}

	public static final class Builder {
		private static final char PATH_SEPARATOR = '/';

		private final String method;
		private final StringBuilder pathBuilder = new StringBuilder( 20 );

		private Map<String, String> parameters;
		private List<ObjectNode> bodyParts;
		private Deadline deadline;
		private final JsonValidation validation = JsonValidation.systemDefault();

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


		/** Retain this validated tree; it must stay unchanged across retries. */
		public Builder bodyJson(ObjectNode object) {
			JsonTreeValidation.validateExternal( object );
			return retainBody( object );
		}

		/**
		 * Internal caller contract: stable ordinary acyclic tree built/parsed by Search or already
		 * validated at the external boundary. Never use for unchecked application/client trees.
		 */
		public Builder bodyJsonFromInternal(ObjectNode object) {
			Objects.requireNonNull( object );
			// Internal checks run at the receiving client's scoped transport boundary.
			return retainBody( object );
		}

		private Builder retainBody(ObjectNode object) {
			if ( bodyParts == null ) {
				bodyParts = new ArrayList<>();
			}
			bodyParts.add( object );
			return this;
		}

		public Builder deadline(Deadline deadline) {
			this.deadline = deadline;
			return this;
		}

		public ElasticsearchJsonRequest build() {
			return new ElasticsearchJsonRequest( this );
		}
	}

}
