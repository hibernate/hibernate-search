/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.common.logging.impl;

public final class MessageConstants {

	private MessageConstants() {
	}

	public static final String PROJECT_CODE = "HSEARCH";

	// -----------------------------------
	// Message ID ranges
	// These ID ranges should NOT OVERLAP
	// -----------------------------------

	public static final int ENGINE_ID_RANGE_MIN = 0;
	public static final int ENGINE_ID_RANGE_MAX = 9999;

	/*
	 * Legacy range from Search 5:
	 * Engine: min = 1, max = 99999 (max used: 0000353)
	 */

	/*
	 * Legacy range from Search 5:
	 * JGroups: min = 200001, max = 299999 (max used: 200025)
	 */

	/*
	 * Legacy range from Search 5:
	 * Avro: min = 300001, max = 399999 (max used: 300002)
	 */

	public static final int BACKEND_ES_ID_RANGE_MIN = 400000;
	public static final int BACKEND_ES_ID_RANGE_MAX = 408999;

	/*
	 * Legacy range from Search 5:
	 * Elasticsearch: min = 400001, max = 499999 (max used: 400094)
	 */

	public static final int BACKEND_ES_AWS_ID_RANGE_MIN = 409000;
	public static final int BACKEND_ES_AWS_ID_RANGE_MAX = 409999;

	public static final int JAKARTA_BATCH_CORE_ID_RANGE_MIN = 500000;
	public static final int JAKARTA_BATCH_CORE_ID_RANGE_MAX = 508999;

	/*
	 * Legacy range from Search 5:
	 * JSR352 integration: min = 500000, max = undefined (max used: 500033)
	 */

	public static final int JAKARTA_BATCH_JBERET_ID_RANGE_MIN = 509000;
	public static final int JAKARTA_BATCH_JBERET_ID_RANGE_MAX = 509999;

	public static final int BACKEND_LUCENE_ID_RANGE_MIN = 600000;
	public static final int BACKEND_LUCENE_ID_RANGE_MAX = 609999;

	public static final int MAPPER_POJO_ID_RANGE_MIN = 700000;
	public static final int MAPPER_POJO_ID_RANGE_MAX = 709999;

	public static final int MAPPER_POJO_STANDALONE_ID_RANGE_MIN = 750000;
	public static final int MAPPER_POJO_STANDALONE_ID_RANGE_MAX = 759999;

	public static final int MAPPER_ORM_ID_RANGE_MIN = 800000;
	public static final int MAPPER_ORM_ID_RANGE_MAX = 809999;

	public static final int MAPPER_ORM_OUTBOX_POLLING_ID_RANGE_MIN = 850000;
	public static final int MAPPER_ORM_OUTBOX_POLLING_ID_RANGE_MAX = 859999;

	public static final int UTIL_ID_RANGE_MIN = 900000;
	public static final int UTIL_ID_RANGE_MAX = 909999;

}
