/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.manualsource.test;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;

/**
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
@Indexed
public class Star {
	private String id;
	private String name;

	@DocumentId
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@Field
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
