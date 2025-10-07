## get tquery

### prerequisites
+ `java` jdk 21
+ `jolie` installed
+ `npm`

```bash
git clone https://github.com/jolie/tquery.git
cd tquery
# tquery's repo ships with gradle 7.0 wrapper
# substitute in place to support Java 21
sed -i 's/gradle-7.0-bin.zip/gradle-8.5-bin.zip/' gradle/wrapper/gradle-wrapper.properties
# build tquery (will create build/libs/tquery-0.4.10.jar w/ java classes)
./gradlew build
# jolie looks for java libs in `lib/`
cd ..
mkdir -p lib
cp tquery/build/libs/tquery-0.4.10.jar lib/

npm init -y

# install tquery package from npm (for jolie module definitions)
npm install @jolie/tquery --save

# create packages directory structure
mkdir -p packages/@jolie

# copy tquery module files to packages directory
cp -r node_modules/@jolie/tquery packages/@jolie/


# create test file
cat > test_tquery.ol << 'EOF'
from @jolie.tquery.main import TQuery
from console import Console

service Test {
    embed TQuery as TQuery
    embed Console as Console

    main {
        testData.records[0].name = "Alice";
        testData.records[1].name = "Bob";

        unwind@TQuery({
            data << testData
            query = "records"
        })(result);

        println@Console("unwind result count: " + #result.result)()
    }
}
EOF

# run test (should output: "unwind result count: 2")
jolie test_tquery.ol

rm test_tquery.ol
```
