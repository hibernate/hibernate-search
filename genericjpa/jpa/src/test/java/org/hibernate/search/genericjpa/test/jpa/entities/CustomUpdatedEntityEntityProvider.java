/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.genericjpa.test.jpa.entities;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.hibernate.search.genericjpa.entity.EntityManagerEntityProvider;

/**
 * Created by Martin on 08.07.2015.
 */
public class CustomUpdatedEntityEntityProvider implements EntityManagerEntityProvider {

	public static final String CUSTOM_TEXT = "customupdated";
	public static final String CUSTOM_TEXT_HINTS = "customupdated_hints";

	@Override
	public Object get(EntityManager em, Class<?> entityClass, Object id, Map<String, Object> hints) {
		CustomUpdatedEntity ret = (CustomUpdatedEntity) em.find( entityClass, id );
		em.detach( ret );

		//we somehow have to check whether this class was used.
		if ( hints != null && hints.containsKey( "testCustomUpdatedEntity" ) && hints.get( "testCustomUpdatedEntity" ).equals( "toast" ) ) {
			ret.setText( CUSTOM_TEXT_HINTS );
		}
		else {
			ret.setText( CUSTOM_TEXT );
		}
		return ret;
	}

	@Override
	public List getBatch(EntityManager em, Class<?> entityClass, List<Object> id, Map<String, Object> hints) {
		List ret = id.stream().map( id_ -> this.get( em, entityClass, id_, hints ) ).collect( Collectors.toList() );
		return ret;
	}

}
