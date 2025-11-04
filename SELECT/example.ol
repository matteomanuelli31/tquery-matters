include "console.iol"

main {
    // person has BOTH a string value AND children
    person = "John Doe";
    person.age = 30;
    person.city = "Rome";

    println@Console( "Person name: " + person )();
    println@Console( "Person age: " + person.age )();
    println@Console( "Person city: " + person.city )();

    // Another example: product
    product = 19.99;
    product.name = "Book";
    product.stock = 100;

    println@Console( "Product price: " + product )();
    println@Console( "Product name: " + product.name )();
    println@Console( "Product stock: " + product.stock )()
}
