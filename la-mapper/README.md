## la-mapper
Data mapper for kotlin 

`LaMapper.copyFrom(orig)`

Create target object and fill values from `orig` object - automatically by name or manually. 
In addition, datatype conversions will be applied to match target type.

### Example

```kotlin
data class Person(
    val id: Int,
    val personCode: Long,
    val regDate: LocalDateTime,
    val firstName: String,
    val surname: String
)

data class PersonDto(
    val id: Long,           // numbers conversion
    val code: String,       // anything to string
    val regDate: LocalDate, // date conversion
    val name: String        // name will construct manually
)

// it is a good style to write converter as extension function
fun Person.toPersonDto() = LaMapper.copyFrom(this) {
    PersonDto::code from Person::personCode
    PersonDto::name from { "${it.firstName} ${it.surname}" }
}

// we have original object
val person = Person(
    id = 101,
    personCode = 123456,
    regDate = LocalDateTime.parse("2022-11-08T12:20:00"),
    firstName = "foo",
    surname = "boo"
)

// copy with transformation
val dto = person.toPersonDto()
println(dto)
```
Will get such `PersonDto` object
> PersonDto(id=101, code=123456, regDate=2022-11-08, name=foo boo)

### Usage

Just add maven dependency
```xml
<dependency>
    <groupId>com.github.labai.utils</groupId>
    <artifactId>la-mapper</artifactId>
    <version>0.1.0</version>
</dependency>
```
and use it.

### Features

- create target object by calling primary constructor
- fill other properties (non constructor arguments)
- automatically map fields by name
- auto conversion between various data types:
  - between numbers (int, long, short, byte, BigDecimal, double, float, Deci)
  - between various date formats
  - enums - string 
  - boolean - number, string
  - additional user data type converters
- possible to add own mapping:
  - simple field to field, or 
  - construct result value from original object
