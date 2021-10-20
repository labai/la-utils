# Deci

Work with decimals in Java is unpleasant.
The float and double classes can be used 
but they use a floating point and are not recommended for cases, 
when fixed point is required, e.g. in financial calculations.
Instead, the BigDecimal class is for such cases.

But unfortunately, a code, written with BigDecimals, is ugly, difficult to read.
In addition, BigDecimal has pitfalls (e.g. equals, lose scale on division).

Luckily, kotlin have operators (- + * /), which should improve readability.
Unfortunately, it still have some problems, derived from BigDecimal, an example:
- div (division) use original scale and the HALF_EVEN rounding is used,
which is not useful in most of situations. Thus, normally you can't use div 
operator (/) in your formulas
- equality check takes into account a scale, so 2.0 != 2.00. So you need to use compareTo instead of "==" check

To solve these problems the *Deci* class can be used. 
Idea is to create simple BigDecimal wrapper, which behaves slightly differently:
- use HALF_UP rounding
- division result with high scale
- additional math operators with BigDecimal, Int, Long
- equal ('==') ignores scale

Few additional functions:
- round - round number by provided count of decimal places, return Deci
- eq - comparison between numbers (various types, including null)
- BigDecimal, Int and Long classes have extension functions *.deci* 
to convert to *Deci*.


#### Examples

```kotlin
val d1: Deci = (price * quantity - fee) * 100 / (price * quantity) round 2
```
```kotlin
val d2: BigDecimal = ((1.deci - 1.deci / 365) * (1.deci - 2.deci / 365) round 11).toBigDecimal()
```

#### Usage
Use maven dependency:

```xml
<dependency>
    <groupId>com.github.labai</groupId>
    <artifactId>deci</artifactId>
    <version>0.0.1</version>
</dependency>
```
