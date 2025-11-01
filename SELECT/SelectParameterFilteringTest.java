import java.util.List;

public class SelectParameterFilteringTest {
	public static void main( String[] args ) {
		Value root = Value.create();
		Value data = root.getChildren( "data" ).first();

		// ===== PATH 0, METHOD 0 (GET) =====
		// param[0]: { in: "query", name: "page", type: "integer" }
		Value p0m0p0 = data.getChildren( "paths" ).get( 0 )
			.getChildren( "methods" ).get( 0 )
			.getChildren( "parameters" ).get( 0 );
		p0m0p0.getChildren( "in" ).first().setValue( "query" );
		p0m0p0.getChildren( "name" ).first().setValue( "page" );
		p0m0p0.getChildren( "type" ).first().setValue( "integer" );

		// param[1]: { in: "header", name: "Authorization", type: "string" }
		Value p0m0p1 = data.getChildren( "paths" ).get( 0 )
			.getChildren( "methods" ).get( 0 )
			.getChildren( "parameters" ).get( 1 );
		p0m0p1.getChildren( "in" ).first().setValue( "header" );
		p0m0p1.getChildren( "name" ).first().setValue( "Authorization" );
		p0m0p1.getChildren( "type" ).first().setValue( "string" );

		// ===== PATH 0, METHOD 1 (POST) =====
		// param[0]: { in: "body", name: "user", schema: "UserSchema" } - NO TYPE!
		Value p0m1p0 = data.getChildren( "paths" ).get( 0 )
			.getChildren( "methods" ).get( 1 )
			.getChildren( "parameters" ).get( 0 );
		p0m1p0.getChildren( "in" ).first().setValue( "body" );
		p0m1p0.getChildren( "name" ).first().setValue( "user" );
		p0m1p0.getChildren( "schema" ).first().setValue( "UserSchema" );

		// param[1]: { in: "header", name: "Content-Type", type: "string" }
		Value p0m1p1 = data.getChildren( "paths" ).get( 0 )
			.getChildren( "methods" ).get( 1 )
			.getChildren( "parameters" ).get( 1 );
		p0m1p1.getChildren( "in" ).first().setValue( "header" );
		p0m1p1.getChildren( "name" ).first().setValue( "Content-Type" );
		p0m1p1.getChildren( "type" ).first().setValue( "string" );

		// ===== PATH 1, METHOD 0 (GET) =====
		// param[0]: { in: "path", name: "id", type: "string" }
		Value p1m0p0 = data.getChildren( "paths" ).get( 1 )
			.getChildren( "methods" ).get( 0 )
			.getChildren( "parameters" ).get( 0 );
		p1m0p0.getChildren( "in" ).first().setValue( "path" );
		p1m0p0.getChildren( "name" ).first().setValue( "id" );
		p1m0p0.getChildren( "type" ).first().setValue( "string" );

		// param[1]: { in: "header", name: "Authorization", type: "string" }
		Value p1m0p1 = data.getChildren( "paths" ).get( 1 )
			.getChildren( "methods" ).get( 0 )
			.getChildren( "parameters" ).get( 1 );
		p1m0p1.getChildren( "in" ).first().setValue( "header" );
		p1m0p1.getChildren( "name" ).first().setValue( "Authorization" );
		p1m0p1.getChildren( "type" ).first().setValue( "string" );

		System.out.println( "=== Heterogeneous structure: some have 'type', some have 'schema' ===" );
		System.out.println( "" );

		// Test 1: Group by .in field
		System.out.println( "Test 1: SELECT WHERE .in = body" );
		List< String > bodyParams = new SelectBuilder()
			.select( "data.paths[*].methods[*].parameters[*]" )
			.from( root )
			.where( ".in = body" )
			.exec();
		System.out.println( "  Result: " + bodyParams );
		boolean test1 = bodyParams.size() == 1;

		System.out.println( "\nTest 2: SELECT WHERE .in = path" );
		List< String > pathParams = new SelectBuilder()
			.select( "data.paths[*].methods[*].parameters[*]" )
			.from( root )
			.where( ".in = path" )
			.exec();
		System.out.println( "  Result: " + pathParams );
		boolean test2 = pathParams.size() == 1;

		System.out.println( "\nTest 3: SELECT WHERE .in = query" );
		List< String > queryParams = new SelectBuilder()
			.select( "data.paths[*].methods[*].parameters[*]" )
			.from( root )
			.where( ".in = query" )
			.exec();
		System.out.println( "  Result: " + queryParams );
		boolean test3 = queryParams.size() == 1;

		System.out.println( "\nTest 4: SELECT WHERE .in = header" );
		List< String > headerParams = new SelectBuilder()
			.select( "data.paths[*].methods[*].parameters[*]" )
			.from( root )
			.where( ".in = header" )
			.exec();
		System.out.println( "  Result: " + headerParams );
		boolean test4 = headerParams.size() == 3;

		// Test 5: Find params with 'schema' field (heterogeneous check)
		System.out.println( "\nTest 5: SELECT WHERE schema in . (has schema field)" );
		List< String > schemaParams = new SelectBuilder()
			.select( "data.paths[*].methods[*].parameters[*]" )
			.from( root )
			.where( "schema in ." )
			.exec();
		System.out.println( "  Result: " + schemaParams );
		System.out.println( "  Expected: only body param has 'schema' instead of 'type'" );
		boolean test5 = schemaParams.size() == 1;

		// Test 6: Find params with 'type' field
		System.out.println( "\nTest 6: SELECT WHERE type in . (has type field)" );
		List< String > typeParams = new SelectBuilder()
			.select( "data.paths[*].methods[*].parameters[*]" )
			.from( root )
			.where( "type in ." )
			.exec();
		System.out.println( "  Result: " + typeParams );
		System.out.println( "  Expected: all except body param (5 total)" );
		boolean test6 = typeParams.size() == 5;

		// Verify all
		if( test1 && test2 && test3 && test4 && test5 && test6 ) {
			System.out.println( "\n✓ ALL TESTS PASSED" );
			System.exit( 0 );
		} else {
			System.out.println( "\n✗ SOME TESTS FAILED" );
			System.exit( 1 );
		}
	}
}
