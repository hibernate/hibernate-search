/**
 * Library-neutral contracts for replayable JSON output and incremental response consumption.
 * Transports own scheduling, compression, HTTP framing, limits and attempt lifecycles.
 * Search supplies response consumer factories; transports pass their generic results through unchanged.
 */
@Incubating
package org.hibernate.search.util.json.spi;

import org.hibernate.search.util.common.annotation.Incubating;
