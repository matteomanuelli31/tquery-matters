import java.util.List;

public class SelectTest {
	public static void main( String[] args ) {
		// Create Jolie tree structure:
		// a.b.c.d.e = 5
		// a.b.e = 5

		Value a = Value.create();
		Value b = a.getChildren( "b" ).first();
		Value c = b.getChildren( "c" ).first();
		Value d = c.getChildren( "d" ).first();
		d.getChildren( "e" ).first().setValue( 5 );

		// a.b.e = 5 (same b as above)
		b.getChildren( "e" ).first().setValue( 5 );

		// Verify structure
		System.out.println( "a.b.c.d.e = " + a.getChildren( "b" ).first()
			.getChildren( "c" ).first()
			.getChildren( "d" ).first()
			.getChildren( "e" ).first().intValue() );

		System.out.println( "a.b.e = " + a.getChildren( "b" ).first()
			.getChildren( "e" ).first().intValue() );

		// Execute SELECT query: select $.b.* from a where ..e = 5
		List< String > results = new SelectBuilder()
			.select( "$.b.*" )
			.from( a, "a" )
			.where( "..e = 5" )
			.exec();

		// Expected output: ["a.b.e", "a.b.c"]
		System.out.println( "\nSELECT results:" );
		for( String path : results ) {
			System.out.println( "  " + path );
		}

		// Verify expected results
		if( results.size() == 2 &&
			results.contains( "a.b.e" ) &&
			results.contains( "a.b.c" ) ) {
			System.out.println( "\n✓ TEST PASSED" );
			System.exit( 0 );
		} else {
			System.out.println( "\n✗ TEST FAILED" );
			System.out.println( "Expected: [a.b.e, a.b.c]" );
			System.out.println( "Got: " + results );
			System.exit( 1 );
		}
	}
}
