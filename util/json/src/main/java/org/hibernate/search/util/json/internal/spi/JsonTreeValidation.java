package org.hibernate.search.util.json.internal.spi;

import org.hibernate.search.util.common.annotation.Incubating;
import org.hibernate.search.util.json.impl.JsonTreeValidator;

import tools.jackson.databind.JsonNode;

/** Validation of stable external ordinary JSON trees; it never serializes, copies or maps Java models. */
@Incubating
public final class JsonTreeValidation {
	/** Opt-in internal invariant checks; standalone utilities default to false. */
	public static final String INTERNAL_VALIDATION_PROPERTY = "hibernate.search.json.internal_validation";

	/** Current JVM fallback for standalone utility calls; configured services retain their own policy. */
	public static boolean isInternalValidationEnabled() {
		return JsonValidation.systemDefault().enabled();
	}

	/** Standalone JVM fallback; only for stable internal or already externally validated trees. */
	public static void validateInternal(JsonNode root) {
		JsonValidation.systemDefault().validateInternal( root );
	}

	private JsonTreeValidation() {
	}

	/**
	 * Accept standard acyclic JSON nodes, including shared acyclic subtrees. Reject missing, POJO, raw,
	 * binary, custom/subclassed and nonfinite nodes. Numeric ranges match the parser policy.
	 * Text-input byte/token constraints do not limit an already constructed exact numeric value.
	 */
	public static void validateExternal(JsonNode root) {
		JsonTreeValidator.validate( root );
	}
}
