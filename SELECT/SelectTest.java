import java.util.List;

public class SelectTest {
	public static void main( String[] args ) {
		// Create Jolie tree structure:
		// a.b.c.d.e = 5
		// a.b.e = 5

		Value root = Value.create();

		// a.b.c.d.e = 5
		Value a = root.getChildren( "a" ).first();
		Value b = a.getChildren( "b" ).first();
		Value c = b.getChildren( "c" ).first();
		Value d = c.getChildren( "d" ).first();
		d.getChildren( "e" ).first().setValue( 5 );

		// a.b.e = 5 (same b as above)
		b.getChildren( "e" ).first().setValue( 5 );

		// Verify structure
		System.out.println( "a.b.c.d.e = " + root.getChildren( "a" ).first()
			.getChildren( "b" ).first()
			.getChildren( "c" ).first()
			.getChildren( "d" ).first()
			.getChildren( "e" ).first().intValue() );

		System.out.println( "a.b.e = " + root.getChildren( "a" ).first()
			.getChildren( "b" ).first()
			.getChildren( "e" ).first().intValue() );

		// Execute SELECT query: select $.a.b.* from root where ..e = 5
		// $ stands for root
		List< String > results = new SelectBuilder()
			.select( "a.b.*" )
			.from( root )
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
