import java.util.List;

public class SelectWherePathTest {
	public static void main( String[] args ) {
		// Create structure:
		// root.items[0].price = 100
		// root.items[1].price = 200

		Value root = Value.create();
		root.getChildren( "items" ).get( 0 )
			.getChildren( "price" ).first().setValue( 100 );
		root.getChildren( "items" ).get( 1 )
			.getChildren( "price" ).first().setValue( 200 );

		System.out.println( "Structure: root.items[0].price=100, root.items[1].price=200\n" );

		// Test: SELECT $.items[*] FROM root WHERE .price = 100
		System.out.println( "Test: SELECT $.items[*] FROM root WHERE .price = 100" );
		List<String> results = new SelectBuilder()
			.select( "$.items[*]" )
			.from( root, "root" )
			.where( ".price = 100" )
			.exec();

		System.out.println( "Results: " + results );
		System.out.println( "Expected: [root.items[0]]" );

		if( results.size() == 1 && results.contains( "root.items[0]" ) ) {
			System.out.println( "✓ TEST PASSED" );
		} else {
			System.out.println( "✗ TEST FAILED" );
			System.out.println( "Got: " + results );
			System.exit( 1 );
		}
	}
}
