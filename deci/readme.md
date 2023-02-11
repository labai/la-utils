## deci.kt
deci.kt was moved to [deci](../../../../deci/) repo

---

---

---

# deci

## deci.kt
[deci.kt](../../tree/main/deci) - decimal class for Kotlin without tricks


### Math in code

With _Deci_ you can use operators, it makes formulas easy readable to compare with method calls for _BigDecimal_

```kotlin
val result = (price * quantity - fee) * 100 / (price * quantity) round 2
```

### BigDecimal vs Deci examples

#### 1. Equals
You would expect numbers are equal independently on decimal zeros.
It is not true with _BigDecimal_:
```kotlin
println(BigDecimal("1.0") == BigDecimal("1"))
```
> false

You need to use the `compareTo` instead of the `equals` for _BigDecimal_.
With _Deci_ it is correct:
```kotlin
println(Deci("1.0") == Deci("1"))
```
> true

#### 2. Dividing
_BigDecimal_ keeps the scale of the first argument when dividing
```kotlin
   println(BigDecimal("5") / BigDecimal("2"))
```
> 2

_Deci_ use high scale - up to 20 decimals for precision or scale
which is enough for most real world cases.

```kotlin
   println(BigDecimal("5") / BigDecimal("2"))
```
> 2.5

```kotlin
   println(BigDecimal("100000") / BigDecimal("3"))
```
> 33333.33333333333333333333

```kotlin
   println(BigDecimal("0.00001") / BigDecimal("3"))
```
> 0.0000033333333333333333333

#### 3. Rounding

_BigDecimal_ use the half-even rounding by default, while _Deci_ - half-up, which is more common

```kotlin
println(BigDecimal("2.5") / BigDecimal("2"))
```
> 1.2

```kotlin
println(Deci("2.5") / Deci("2") round 1)
```
> 1.3

