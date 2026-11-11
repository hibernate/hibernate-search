package org.hibernate.search.util.json.internal.spi;

import org.hibernate.search.util.common.annotation.Incubating;
import org.hibernate.search.util.json.impl.JsonTreeValidator;

import tools.jackson.databind.JsonNode;

/** Immutable internal invariant checks for one JSON service context. External trees always require validation. */
@Incubating
public final class JsonValidation {
	private static final JsonValidation ENABLED = new JsonValidation( true );
	private static final JsonValidation DISABLED = new JsonValidation( false );

	public static JsonValidation of(boolean enabled) {
		return enabled ? ENABLED : DISABLED;
	}

	/** Snapshot the standalone utility default when creating a service, body or cursor. */
	public static JsonValidation systemDefault() {
		String utilityDefault = System.getProperty( JsonTreeValidation.INTERNAL_VALIDATION_SYSTEM_PROPERTY );
		return of( utilityDefault == null
				? Boolean.getBoolean( JsonTreeValidation.INTERNAL_VALIDATION_PROPERTY )
				: Boolean.parseBoolean( utilityDefault ) );
	}

	private final boolean enabled;

	private JsonValidation(boolean enabled) {
		this.enabled = enabled;
	}

	public boolean enabled() {
		return enabled;
	}

	/** Only for stable trees built/parsed internally or already validated at their external boundary. */
	public void validateInternal(JsonNode root) {
		if ( enabled ) {
			JsonTreeValidator.validate( root );
		}
	}
}
