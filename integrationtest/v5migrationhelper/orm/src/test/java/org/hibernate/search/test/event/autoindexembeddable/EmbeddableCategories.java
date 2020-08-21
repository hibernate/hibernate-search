/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.event.autoindexembeddable;

import java.util.HashMap;
import java.util.Map;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.MapKeyColumn;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FieldBridge;
import org.hibernate.search.annotations.IndexedEmbedded;

/**
 * @author Hardy Ferentschik
 */
@Embeddable
public class EmbeddableCategories {

	private Map<Long, String> categories;

	@ElementCollection
	@IndexedEmbedded
	@MapKeyColumn
	@Field(bridge = @FieldBridge(impl = CategoriesBridge.class))
	public Map<Long, String> getCategories() {
		if ( categories == null ) {
			categories = new HashMap<Long, String>();
		}
		return categories;
	}

	public void setCategories(Map<Long, String> categories) {
		this.categories = categories;
	}
}


