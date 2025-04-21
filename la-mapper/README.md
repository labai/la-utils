
# la-mapper

**la-mapper** is a lightweight Kotlin and Java-friendly library for flexible, concise data mapping.
It simplifies DTO transformation by allowing automatic field mapping and intuitive custom mappings,
with support for basic type conversions and both Kotlin and Java interoperability.
It supports:

- **Automatic field mapping** based on property names.
- **Manual Field Mapping** via lambda functions.
- **Data type conversions** for compatible types (e.g., `Int` to `Long`, `LocalDateTime` to `LocalDate`).
- **Kotlin & Java Support** — works seamlessly in both

## Examples

### Kotlin example

```kotlin
// source class
data class Person(
    val id: Int,
    val personCode: Long,
    val regDate: LocalDateTime,
    val firstName: String,
    val surname: String
)

// target class
data class PersonDto(
    val id: Long,           // numbers conversion
    val code: String,       // anything to string
    val regDate: LocalDate, // date conversion
    val name: String        // name will construct manually
)

// it is a good style to write converter as extension function
fun Person.toPersonDto(): PersonDto = LaMapper.copyFrom(this) {
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

### Java example

You can use `la-mapper` just as easily in Java using `LaMapperJ`.

```java
public class Person {
    public Integer id;
    public Long personCode;
    public LocalDateTime regDate;
    public String firstName;
    public String surname;
}

public class PersonDto {
    public Long id;
    public String code;
    public LocalDate regDate;
    public String name;
}

public void transformPerson() {
    Person from = new Person();
    from.id = 101;
    from.personCode = 123456L;
    from.regDate = LocalDateTime.parse("2022-11-08T12:20:00");
    from.firstName = "Foo";
    from.surname = "Boo";

    PersonDto to = LaMapperJ.copyFrom(from, PersonDto.class, List.of(
        mapFrom("code", f -> f.personCode),
        mapFrom("name", f -> f.firstName + " " + f.surname)
    ));
}
```

## Installation

To include `la-mapper` in your project, add the following dependency:

<details>
<summary><strong>Maven</strong></summary>

```xml
<dependency>
  <groupId>com.github.labai.utils</groupId>
  <artifactId>la-mapper</artifactId>
  <version>0.2.3</version>
</dependency>
```
</details>

<details>
<summary><strong>Gradle (Kotlin DSL)</strong></summary>

```kotlin
implementation("com.github.labai.utils:la-mapper:0.2.3")
```
</details>

## Features
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

## Performance
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

## Limitation
- for simple classes, inheritance hierarchy is not supported
- doesn't support collection mapping - usually it is better to map them manually using kotlin collection mapping functions

## Additional features

### Helpers `f` and `t` for method references

A long class names mapping may look bit clumsy, e.g. 

```kotlin
fun HighlyRespectedPerson.toPersonDto(): PersonApiResponseDto = LaMapper.copyFrom(this) {
    PersonApiResponseDto::code from HighlyRespectedPerson::personCode
    PersonApiResponseDto::name from HighlyRespectedPerson::fullName
}
```
So now it is possible to write the mapping in a shorter manner
```kotlin
fun HighlyRespectedPerson.toPersonDto(): PersonApiResponseDto = LaMapper.copyFrom(this) {
    t::code from f::personCode
    t::name from f::fullName
}
```
Special pseudo-variables are used here:
- `t` - for accessing target ("to") class fields
- `f` - for accessing source ("from") class fields

NB: they are created for field references only and can't be used directly as object, 
i.e. trying to write `t.code = f.personCode` would cause an error. 
