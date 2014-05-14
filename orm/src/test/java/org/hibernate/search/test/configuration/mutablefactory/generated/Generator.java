/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.configuration.mutablefactory.generated;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * @author Emmanuel Bernard
 */
public final class Generator {

	private Generator() {
		//not allowed
	}

	public static void main(String[] args) {
		StringBuilder generated = new StringBuilder( );
		generated.append( "package org.hibernate.search.test.configuration.mutablefactory.generated;\n\n" )
			.append( "import org.hibernate.search.annotations.DocumentId;\n" )
			.append( "import org.hibernate.search.annotations.Field;\n" )
			.append( "import org.hibernate.search.annotations.Indexed;\n\n" )
			.append( "/** Class generated container 100 inner classes */" )
			.append( "public class Generated {\n" );

		StringBuilder inner = new StringBuilder( );
		inner.append( "\t" ).append( "@Indexed").append( "\n" )
				.append( "\t" ).append( "public static class Ax {" ).append( "\n" )
				.append( "\t" ).append( "\t" ).append( "public Ax(Integer id, String name) { this.id = id; this.name = name; }" ).append( "\n\n" )
				.append( "\t" ).append( "\t" ).append( "@DocumentId" ).append( "\n" )
				.append( "\t" ).append( "\t" ).append( "public Integer getId() {return id;}" ).append( "\n" )
				.append( "\t" ).append( "\t" ).append( "public void setId(Integer id) { this.id = id; }" ).append( "\n" )
				.append( "\t" ).append( "\t" ).append( "private Integer id;" ).append( "\n\n" )
				.append( "\t" ).append( "\t" ).append( "@Field" ).append( "\n" )
				.append( "\t" ).append( "\t" ).append( "public String getName() {return name;}" ).append( "\n" )
				.append( "\t" ).append( "\t" ).append( "public void setName(String name) { this.name = name; }" ).append( "\n" )
				.append( "\t" ).append( "\t" ).append( "private String name;" ).append( "\n" )
				.append( "\t}\n\n" );
		String innerString = inner.toString();
		for ( int i = 0 ; i < 100 ; i++ ) {
			generated.append( innerString.replace( "Ax", "A" + i ) );
		}

		generated.append( "}\n" );
		File f = new File( "./Generated.java" );
		try {
			FileWriter fw = new FileWriter( f );
			fw.write( generated.toString() );
			fw.close();
		}
		catch (IOException e) {
			System.out.println( "Error while generating classes" );
			e.printStackTrace();
		}
		System.out.println( "Generated in :" + f.getAbsolutePath() );
	}
}
