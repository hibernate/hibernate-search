/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.bridge.dependencies.containers.fromotherentity;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.query.Query;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.mapper.orm.HibernateOrmExtension;
import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.TypeBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.TypeBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.TypeBridgeWriteContext;

//tag::include[]
public class ScientificPapersReferencedByBinder implements TypeBinder {

	@Override
	public void bind(TypeBindingContext context) {
		context.dependencies()
				.fromOtherEntity( ScientificPaper.class, "references" ) // <1>
				.use( "title" ); // <2>

		IndexFieldReference<String> papersReferencingThisOneField = context.indexSchemaElement()
				.field( "referencedBy", f -> f.asString().analyzer( "english" ) )
				.multiValued()
				.toReference();

		context.bridge( new Bridge( papersReferencingThisOneField ) );
	}

	private static class Bridge implements TypeBridge {

		private final IndexFieldReference<String> referencedByField;

		private Bridge(IndexFieldReference<String> referencedByField) { // <2>
			this.referencedByField = referencedByField;
		}

		@Override
		public void write(DocumentElement target, Object bridgedElement, TypeBridgeWriteContext context) {
			ScientificPaper paper = (ScientificPaper) bridgedElement;

			for ( String referencingPaperTitle : findReferencingPaperTitles( context, paper ) ) { // <3>
				target.addValue( referencedByField, referencingPaperTitle );
			}
		}

		private List<String> findReferencingPaperTitles(TypeBridgeWriteContext context, ScientificPaper paper) {
			Session session = context.extension( HibernateOrmExtension.get() ).session();
			Query<String> query = session.createQuery(
					"select p.title from ScientificPaper p where :this member of p.references",
					String.class );
			query.setParameter( "this", paper );
			return query.list();
		}
	}
}
//end::include[]
