# la-mapper

**la-mapper** is a lightweight Kotlin and Java-friendly library for flexible, concise, and automatic data mapping.  
It simplifies DTO transformations by providing automatic field copying, intuitive custom mappings, 
and built-in type conversions — with full interoperability across Kotlin and Java.

## Features

- **Automatic field mapping**  
  Automatically maps fields with matching names.

- **Custom field mapping**  
  Define custom transformations via lambdas or property references.

- **Built-in data type conversions**  
  Supports basic type conversions out of the box, including
  `Int → Long`, `Number → String`, `LocalDateTime → LocalDate`, etc.

- **Kotlin & Java interoperability**  
  Kotlin DSL with extension functions, plus a Java-friendly API via `LaMapperJ`.

# Examples

## Kotlin example

---

## 1. Creating a new object based on source object

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
    val id: Long,           // numeric conversion
    val code: String,       // any type to string
    val regDate: LocalDate, // date conversion
    val name: String        // custom field
)

// it is a good style to write converter as an extension function
fun Person.toPersonDto(): PersonDto = LaMapper.copyFrom(this) {
    PersonDto::code from Person::personCode
    PersonDto::name from { "${it.firstName} ${it.surname}" }
}

// we have the original object
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
Resulting object:
> PersonDto(id=101, code=123456, regDate=2022-11-08, name=foo boo)

## 2 Using shortcuts `f` and `t` for property references

When classes have long names, mappings may get verbose:

```kotlin
fun HighlyRespectedPerson.toPersonDto(): PersonApiResponseDto = LaMapper.copyFrom(this) {
    PersonApiResponseDto::code from HighlyRespectedPerson::personCode
    PersonApiResponseDto::name from HighlyRespectedPerson::fullName
}
```

A shorter form is available using pseudo-variables f and t:

```kotlin
fun HighlyRespectedPerson.toPersonDto(): PersonApiResponseDto = LaMapper.copyFrom(this) {
    t::code from f::personCode
    t::name from f::fullName
}
```
Where:
- `t` - refers to **target** ("to") class fields
- `f` - refers to **source** ("from") class fields

⚠️ _They are only for property references — not real objects._
Expressions like t.code = f.personCode are invalid.


## 3.  Copying into an existing object

```kotlin
LaMapper.copyFields(from, to) {
    exclude(t::name)
    t::address from f::clientAddress
}
```
This:
- automatically copies matching fields
- allows excluding selected fields
- supports overriding or adding custom mapping

## 4. Using LaCopyable for object cloning

Classes that implement `LaCopyable` get free copying via `laCopy()`:

```kotlin
class Sample(val a1: String) : LaCopyable {
    var a2: String? = null
}
val sample = Sample("A1").apply { a2 = "A2" }
val copy: Sample = sample.laCopy()
```

## Java example

The Java API is provided through LaMapperJ and uses functional-style mappings:

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

Add the dependency to your project:

<details>
<summary><strong>Maven</strong></summary>

```xml
<dependency>
  <groupId>com.github.labai.utils</groupId>
  <artifactId>la-mapper</artifactId>
  <version>0.3.1</version>
</dependency>
```
</details>

<details>
<summary><strong>Gradle (Kotlin DSL)</strong></summary>

```kotlin
implementation("com.github.labai.utils:la-mapper:0.3.1")
```
</details>

