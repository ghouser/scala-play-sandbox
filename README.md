# Scala Sandbox
App for experimenting with Scala Play framework.
## Structure
```text
// folders
app                      → Application sources
 └ controllers           → Application controllers
 └ models                → Application data models
 └ services              → Application services
 └ views                 → Templates
conf                     → Configurations files and other non-compiled resources (on classpath)
 └ application.conf      → Main configuration file
 └ routes                → Routes definition
public                   → Public assets
 └ stylesheets           → CSS files
 └ javascripts           → Javascript files
 └ images                → Image files
project                  → sbt configuration files
 └ build.properties      → Marker for sbt project
 └ plugins.sbt           → sbt plugins including the declaration for Play itself
logs                     → Logs folder
 └ application.log       → Default log file
target                   → Generated stuff
 └ resolution-cache      → Info about dependencies
 └ scala-2.13
    └ api                → Generated API docs
    └ classes            → Compiled class files
    └ routes             → Sources generated from routes
    └ twirl              → Sources generated from templates
 └ universal             → Application packaging
 └ web                   → Compiled web assets
test                     → source folder for unit or functional tests
// files
build.sbt                → Application build script
docker-compose.yml       → YML formatted scripts for running docker
Dockerfile               → Creates image for application
init.sql                 → SQL script to initialize Postgres Database
```
# Parse CSV
API tool to parse a CSV file from POST call
- Reply contains parsed data in JSON format 
  - See [Reply Format](https://github.com/ghouser/scala-play-sandbox#reply-format)
  - Data matching tabular structure returned a `"header":"value"` format
  - Data not matching tabular structure returned as raw value
- Successfully parsed data is stored in a Postgres SQL Database
  - See [Postgres Database](https://github.com/ghouser/scala-play-sandbox#postgres-database)

## Quickstart
Dependencies:
* Clone repository
* Docker

Start server:
1. `docker-compose up` will create:
    1. `scala-play-sandbox`
        1. `scala-play-sandbox_sbt_1` - main app
        1. `scala-play-sandbox_db_1` - postgres sql database

1. SBT will automatically run, starting the server. Expected output:
    ```
    --- (Running the application, auto-reloading is enabled) ---
    
    [info] p.c.s.AkkaHttpServer - Listening for HTTP on /0.0.0.0:9000
    ```
1. Access the server at `http://localhost:9000/`
1. clean up with `docker-compose down --volumes`

## Endpoints
### `GET: /checkDB`
Endpoint to check the database is connectable and initialized.
Queries the postgres database for tables created during initialization. On a successful query, endpoint replies OK.

### `POST: /parseCsv`
- header - `Content-Type: multipart/form-data`
- body - `upload=targetFileContent`

Uploaded file can have any name, but must be called `upload` in the POST body.

### Example Call
```
curl -F upload=@simpleTest.csv localhost:9000/parseCsv -H 'Content-Type: multipart/form-data'
```

### Accepted Input Formats
- plain text document
- content in a tabular form
  - rows
    - first row is the header
    - subsequent rows are data
  - columns
    - delimiter is comma `,`
    - other delimiters will result in parsing misinterpreting input has having a single column.
      Data will parse, and no warnings or errors will be given.
  - cells
    - can be wrapped in `" "`, e.g. `"value one"`
    - can contain a quoted portion e.g. `"like "this" example"`
    - can be a mix of double quote wrapped and not across a row e.g. `"value,1"  ,value2  ,"value"3""`
- Illegal Characters
  - Free standing `"` e.g `don"t do this`

#### Example
```text
-- acceptable input parse to rows object
"header,1" ,header2 ,"header"3""
"value,1"  ,value2  ,"value"3""
"value,21" ,value22 ,"value"23""
"value,31" ,value32 ,"value"33""
-- bad input parse to error object
has, too, many, columns
internal, double"quote, fails
```

### Reply Format
JSON object:
```
{
"metadata":{
    "importName":"csv_3039cf951de125a9fb00e92a836c5543",
    "numRecords":Int // total number of records, does not count header
},
"rows":[
    {
        "header1":"value1",
        "header2":"value2",
        "header3":"value3",
        . . . // continue for all columns
    },
   . . . // conintue for all rows
],
"errors":[
    {
        "row":"Int", // row number of rejected record, counting header as row 0
        "value":"full row rejected" // text of rejected record
    },
    . . . // continue for all error records
]
}
```

Example from above input:
```json
{
"metadata":{
    "importName":"csv_3039cf951de125a9fb00e92a836c5543",
    "numRecords":5
},
"rows":[
    {
        "header,1":"value,1",
        "header2":"value2",
        "header\"3\"":"value\"3\""
    },
    {
        "header,1":"value,21",
        "header2":"value22",
        "header\"3\"":"value\"23\""
    },
    {
        "header,1":"value,31",
        "header2":"value32",
        "header\"3\"":"value\"33\""
    }
],
"errors":[
    {
        "row":"4",
        "value":"has, too, many, columns"
    },
    {
        "row":"5",
        "value":"internal, double\"quote, fails"
    }
]
}
```

## Unit Tests
`test/controllers/CsvControllSpec`
- Run with sbt shell, `sbt test`
- Unit test coverage for all `CsvController.parseUtils` functions
- Tests cover data handling as described in [Accepted Input Formats](https://github.com/ghouser/scala-play-sandbox#accepted-input-formats)
- File matching above example can be found at `test/resources/quoteTest.csv`

## Postgres Database
Calls to `parseCSV` also write out to a postgres database.

### Schema Design
- Every import will create a new table with name matching the import name
    - Column names are header row values, limited to only alphanumeric characters
    - Columns are `VARCHAR` without character limits
    - Insert are row by row (not batch inserted)
- Every import will add a record to `importdata` tabl:
    - `id` - autogenerated PK
    - `importName` - unique name given to the import. This is also the table name.
    - `dateCreate` - timestamp of the import
    
## DEV Setup
To run and build locally (without Docker).
### Run with SBT
* `sbt run` - builds and launches app
* `sbt test` - builds and runs tests
  
### Database Setup
Steps for manually creating a postgres database.
1. create a postgres SQL database (command line or any tool)
    1. Setup values need to match `application.conf`
    ```scala
    POSTGRES_USER=postgres
    POSTGRES_PASSWORD=postgres
    POSTGRES_DB=postgres
    POSTGRES_PORT=5432
    ```
    1.  `application.conf` has a URL configured as such, and may need to be modified if local database is not at `localhost`:
    ```scala
    # default URL to local host
    db.default.url="jdbc:postgresql://localhost:5432/postgres"
    # DATABASE_URL env variable set in docker-compose
    # expected value "jdbc:postgresql://db:5432/postgres"
    db.default.url=${?DATABASE_URL}
    ```
1. create schema and `importData` table
    ```sql
    CREATE SCHEMA IF NOT EXISTS scala_play_sandbox
   
   CREATE TABLE IF NOT EXISTS scala_play_sandbox.importdata (
      id serial primary key
      ,importName varchar(100) NOT NULL
      ,dateCreated timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP
    )
    ```
1. Run application and hit endpoint `checkDB`
    1. It queries the `importData` table. Proper DB configuration will return successfully.