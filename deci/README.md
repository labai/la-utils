# Deci

Work with decimals in Java is unpleasant.
The float and double classes can be used 
but they use floating point and are not recommended for cases, 
where fixed point is required, e.g. in financial calculations.
Instead, the BigDecimal class is for working with decimal numbers.

But unfortunately, a code, written with BigDecimals, is ugly, difficult to read.
And in addition, it has pitfalls.  

Luckily, kotlin have operators (- + * /), which should improve readability.
Unfortunately, it still have some problems, derived from BigDecimal, an example:
- div (division) use original scale and the HALF_EVEN rounding is used,
which is not useful in many countries. Thus, normally you can't use div 
operator (/) in your formulas
- equality check takes into account a scale, so "2.0" != "2.00". 
So you need to use compareTo instead of "==" check
- you cannot work with nulls

To solve these problems the *Deci* class can be used. 
Idea is to do simple BigDecimal wrapper, 
which behaves slightly differently for some cases:
- use HALF_UP rounding
- division result with high scale (30)
- additional math operators with BigDecimal, Int, Long
- null support
- equal ('==') ignores scale (uses compareTo)

Few additional functions:
- round - round number by provided number of decimal, return Deci
- bigd - round and convert to BigDecimal
- eq - comparison between numbers (various types, including null)
- BigDecimal, Int and Long classes have extension functions *.deci* 
to convert to *Deci*.


####Examples

```kotlin
val d1: Deci = (price * quantity - fee) * 100 / (price * quantity) round 2
```
```kotlin
val d2: BigDecimal = (1.deci - 1.deci / 365) * (1.deci - 2.deci / 365) bigd 11
```

####Usage
Deci is in experimental stage, but you can copy Deci.kt to your project

