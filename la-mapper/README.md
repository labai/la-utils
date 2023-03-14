## la-mapper
Data mapper for kotlin 

```
LaMapper.copyFrom(orig) {
  // with additional manual mappings
}
```

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
    <version>0.2.1</version>
</dependency>
```
and use it.

### Features
- uses **compiling** at runtime
- creates a target object by calling a primary constructor, kotlin constructors with default parameters also are supported
  - argument can be mapped manually or automatically by field name
- fills properties (non constructor parameters)
  - can be mapped manually or automatically by field name
- auto conversion between various data types:
  - between numbers (int, long, short, byte, BigDecimal, double, float, Deci), including kotlin unsigned numbers
  - between various date formats
  - enums - string 
  - boolean - number, string
  - value class
- additional user defined data type conversion
- possible mapping ways:
  - simple field to field 
  - construct result value from original object

### Performance
LaMapper compiles a mapping code at runtime. So performance is similar to handwritten code.
Same test examples in _test/java/performance_.

| Case                        | Handwritten<br/>code | LaMapper | Reflection              | 
|-----------------------------|----------------------|----------|-------------------------|
| Properties copy             | 27                   | **40**   | 1800                    | 
| Constructor with defaults   | 29                   | **39**   | 2500                    |
| Constructor with all params | 29                   | **41**   | 800                     |

Testing is done by copying of 1mi rows with 20 fields. Here is time in miliseconds:
- _Handwritten code_ - hardcoded assigns. 
- _LaMapper_ - LaMapper compiled mapper (default mode).
- _Reflection_ - alternative LaMapper mode - with pure reflection. It mostly depends on reflection performance.

### Limitation
- for simple classes, inheritance hierarchy is not supported
- doesn't support collection mapping - usually it is better to map them manually using kotlin collection mapping functions
