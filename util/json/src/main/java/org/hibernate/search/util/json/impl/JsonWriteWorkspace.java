package org.hibernate.search.util.json.impl;

import java.util.IdentityHashMap;

import tools.jackson.databind.JsonNode;

/**
 * Single-traversal scratch reused across sequential NDJSON parts, never across cursors.
 */
final class JsonWriteWorkspace {

	char[] integral;

	IdentityHashMap<JsonNode, Boolean> active;

	char[] integral() {
		if ( integral == null ) {
			integral = new char[JsonNumbers.MAX_LONG_CHARACTER_LENGTH];
		}
		return integral;
	}

	void clearReferences() {
		if ( active != null ) {
			active.clear();
		}
	}
}
