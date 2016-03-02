/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.genericjpa.test.metadata;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;

/**
 * @author Martin Braun
 */
public class SubEntity {

	@DocumentId(name = "SUB_NOT_NAMED_ID")
	@Field(name = "SUB_IGNORE_ME")
	private Long subId;

	@Field(analyze = Analyze.NO)
	private String name;

}
