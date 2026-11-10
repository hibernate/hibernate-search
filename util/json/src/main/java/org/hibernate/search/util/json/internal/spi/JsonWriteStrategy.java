package org.hibernate.search.util.json.internal.spi;

import org.hibernate.search.util.common.annotation.Incubating;

/** Strategy for blocking writes to a caller-owned stream. */
@Incubating
public enum JsonWriteStrategy {
	/** Drain an independent cursor using bounded buffer storage. */
	CURSOR,
	/** Use the body's native writer when available, otherwise drain a cursor. */
	NATIVE
}
