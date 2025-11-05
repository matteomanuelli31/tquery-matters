import java.util.List;

public class SelectDescendantArrayTest {
        public static void main( String[] args ) {
                // Create structure:
                // a.b.c.d[0] = 3
                // a.b.c.d[1] = 5
                // a.d[0] = 7
                // a.d[1] = 5

                Value root = Value.create();
                Value a = root.getChildren( "a" ).first();

                // a.b.c.d array
                Value b = a.getChildren( "b" ).first();
                Value c = b.getChildren( "c" ).first();
                ValueVector dArray1 = c.getChildren( "d" );
                dArray1.get( 0 ).setValue( 3 );
                dArray1.get( 1 ).setValue( 5 );

                // a.d array
                ValueVector dArray2 = a.getChildren( "d" );
                dArray2.get( 0 ).setValue( 7 );
                dArray2.get( 1 ).setValue( 5 );

                // Verify structure
                System.out.println( "Structure created:" );
                System.out.println( "  a.b.c.d[0] = " + root.getChildren( "a" ).first()
                        .getChildren( "b" ).first()
                        .getChildren( "c" ).first()
                        .getChildren( "d" ).get( 0 ).intValue() );
                System.out.println( "  a.b.c.d[1] = " + root.getChildren( "a" ).first()
                        .getChildren( "b" ).first()
                        .getChildren( "c" ).first()
                        .getChildren( "d" ).get( 1 ).intValue() );
                System.out.println( "  a.d[0] = " + root.getChildren( "a" ).first()
                        .getChildren( "d" ).get( 0 ).intValue() );
                System.out.println( "  a.d[1] = " + root.getChildren( "a" ).first()
                        .getChildren( "d" ).get( 1 ).intValue() );

                // Test 1: SELECT ..d WHERE . = 5
                // Should return field names where d[0] = 5 (none match)
                System.out.println( "\nTest 1: SELECT ..d WHERE . = 5" );
                List<String> results1 = new SelectBuilder()
                        .select( "..d" )
                        .from( root )
                        .where( ". = 5" )
                        .exec();

                System.out.println( "Results: " + results1 );
                System.out.println( "Expected: [] (d[0] checks: 3≠5, 7≠5)" );

                if( results1.size() == 0 ) {
                        System.out.println( "✓ Test 1 PASSED" );
                } else {
                        System.out.println( "✗ Test 1 FAILED" );
                        System.exit( 1 );
                }

                // Test 2: SELECT ..d[*] WHERE . = 5
                // Should return all array elements where value = 5
                System.out.println( "\nTest 2: SELECT ..d[*] WHERE . = 5" );
                List<String> results2 = new SelectBuilder()
                        .select( "..d[*]" )
                        .from( root )
                        .where( ". = 5" )
                        .exec();

                System.out.println( "Results: " + results2 );
                System.out.println( "Expected: [a.b.c.d[1], a.d[1]]" );

                if( results2.size() == 2 &&
                    results2.contains( "a.b.c.d[1]" ) &&
                    results2.contains( "a.d[1]" ) ) {
                        System.out.println( "✓ Test 2 PASSED" );
                } else {
                        System.out.println( "✗ Test 2 FAILED" );
                        System.out.println( "Got: " + results2 );
                        System.exit( 1 );
                }

                // Test 3: SELECT ..d[*] with no WHERE
                // Should return all d array elements
                System.out.println( "\nTest 3: SELECT ..d[*] (no WHERE)" );
                List<String> results3 = new SelectBuilder()
                        .select( "..d[*]" )
                        .from( root )
                        .exec();

                System.out.println( "Results: " + results3 );
                System.out.println( "Expected: [a.b.c.d[0], a.b.c.d[1], a.d[0], a.d[1]]" );

                if( results3.size() == 4 &&
                    results3.contains( "a.b.c.d[0]" ) &&
                    results3.contains( "a.b.c.d[1]" ) &&
                    results3.contains( "a.d[0]" ) &&
                    results3.contains( "a.d[1]" ) ) {
                        System.out.println( "✓ Test 3 PASSED" );
                } else {
                        System.out.println( "✗ Test 3 FAILED" );
                        System.out.println( "Got: " + results3 );
                        System.exit( 1 );
                }

                System.out.println( "\n✓ ALL TESTS PASSED" );
        }
}
