package org.hibernate.search.backend.elasticsearch.client.common.authentication.spi;

import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionStage;

import org.hibernate.search.backend.elasticsearch.logging.spi.ElasticsearchClientLog;
import org.hibernate.search.util.common.annotation.Incubating;
import org.hibernate.search.util.json.spi.JsonBody;

/**
 * Asynchronous authentication/preparation of one destination attempt, before any bytes are sent.
 * Implementations must be shareable and return promptly; blocking work uses the integration's execution capability.
 * Completion may occur on any thread: only the integration may apply results to its transport, on its owning context.
 */
@Incubating
@FunctionalInterface
public interface ElasticsearchRequestPreparation {
	CompletionStage<Result> prepare(Request request, Execution execution);

	/**
	 * Immutable effective wire metadata, captured after destination selection and user configuration.
	 * URI includes the effective authority and encoded path/query; no user info or fragment is permitted.
	 * Hostnames containing underscores are supported; use {@link Request#host()} and {@link Request#port()}
	 * instead of URI's server-authority accessors, which do not recognize these names.
	 * Header names/values are copied, but the optional body is retained: null means absent, not JSON null.
	 * Body and all underlying values must remain stable through preparation, sending and retries.
	 * Workers must never receive or access a mutable transport request through this snapshot.
	 */
	record Request(URI uri, String method, Map<String, List<String>> headers, JsonBody body) {
		public Request {
			Objects.requireNonNull( uri );
			String authority = uri.getRawAuthority();
			if ( !( "http".equalsIgnoreCase( uri.getScheme() ) || "https".equalsIgnoreCase( uri.getScheme() ) )
					|| authority == null || authority.indexOf( '@' ) >= 0
					|| parsedAuthority( uri ).getHost() == null || uri.getRawFragment() != null ) {
				throw ElasticsearchClientLog.INSTANCE.invalidRequestUri();
			}
			if ( Objects.requireNonNull( method ).isBlank() ) {
				throw ElasticsearchClientLog.INSTANCE.emptyRequestMethod();
			}
			headers = copyHeaders( headers );
		}

		public Request(URI uri, JsonBody body) {
			this( uri, "POST", Map.of(), body );
		}

		public Request withHeaders(Map<String, List<String>> replacement) {
			return new Request( uri, method, replacement, body );
		}

		/** Effective host, preserving underscore-containing names and bracketed IPv6 literals. */
		public String host() {
			if ( uri.getHost() != null ) {
				return uri.getHost();
			}
			return uri.getRawAuthority().substring( 0, parsedAuthority( uri ).getHost().length() );
		}

		/** Explicit port, or {@code -1} when the scheme's default applies. */
		public int port() {
			return parsedAuthority( uri ).getPort();
		}

		private static URI parsedAuthority(URI uri) {
			if ( uri.getHost() != null || uri.getRawAuthority() == null ) {
				return uri;
			}
			// Validate only the authority, allowing underscores wherever a hostname letter is allowed.
			// The surrogate has the same length; host() always returns the original spelling.
			return URI.create( "http://" + uri.getRawAuthority().replace( '_', 'a' ) );
		}
	}

	/** Complete immutable header replacement. Method, destination and body cannot change during preparation. */
	record Result(Map<String, List<String>> headers) {
		public Result {
			headers = copyHeaders( headers );
		}
	}

	/**
	 * Owned/configured by the integration, not by Search or an authentication provider. No implicit executor is used.
	 * Implementations must be safe to call from preparation callbacks and the blocking action's thread.
	 */
	interface Execution {
		/**
		 * Schedule on the explicitly configured blocking facility, with admission/rejection policy owned by the integration.
		 * The action owns any streams/cursors it opens and must close them on its own thread, including after cancellation.
		 * Returning/cancelling a stage does not imply interruption of arbitrary blocking credential providers.
		 */
		<T> CompletionStage<T> blocking(Callable<T> action);

		/**
		 * Throw when the attempt is cancelled, expired or superseded. Must be thread-safe and never touch transport state.
		 * The integration also rejects late completions before sending/applying headers; cancelling a stage alone is insufficient.
		 */
		void checkActive();
	}

	private static Map<String, List<String>> copyHeaders(Map<String, List<String>> headers) {
		Map<String, List<String>> copy = new LinkedHashMap<>();
		Objects.requireNonNull( headers )
				.forEach( (name, values) -> copy.put( Objects.requireNonNull( name ), List.copyOf( values ) ) );
		return Collections.unmodifiableMap( copy );
	}
}
