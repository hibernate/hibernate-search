/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
@FullTextFilterDefs({
	@FullTextFilterDef(
		name = "package-filter-1",
		impl = RoleFilterFactory.class
	),
	@FullTextFilterDef(
		name = "package-filter-2",
		impl = RoleFilterFactory.class
	)
})
package org.hibernate.search.test.filter.fulltextfilterdefs;

import org.hibernate.search.annotations.FullTextFilterDef;
import org.hibernate.search.annotations.FullTextFilterDefs;
import org.hibernate.search.test.filter.RoleFilterFactory;

