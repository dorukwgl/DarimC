# Darim Language Reference

> **Darim** is a statically-typed, VM-executed language designed for exploring compiler frontend design, register-based virtual machine architecture, custom memory management, and runtime engineering. It is expressive, pragmatic, and built from scratch — including its own lexer, parser, AST, bytecode compiler, and register VM.

---

## Table of Contents

1. [Introduction](#1-introduction)
2. [Getting Started](#2-getting-started)
3. [Variables & Constants](#3-variables--constants)
4. [Data Types](#4-data-types)
   - 4.1 [Numbers](#41-numbers)
   - 4.2 [Booleans](#42-booleans)
   - 4.3 [Strings](#43-strings)
   - 4.4 [Arrays](#44-arrays)
   - 4.5 [Tuples](#45-tuples)
   - 4.6 [Maps](#46-maps)
   - 4.7 [Sets](#47-sets)
   - 4.8 [Enums](#48-enums)
5. [Operators & Expressions](#5-operators--expressions)
6. [Control Flow](#6-control-flow)
   - 6.1 [If / Elif / Else](#61-if--elif--else)
   - 6.2 [For Loops](#62-for-loops)
   - 6.3 [While Loops](#63-while-loops)
   - 6.4 [Match Expression](#64-match-expression)
7. [Functions](#7-functions)
8. [Lambdas](#8-lambdas)
9. [Memory Model](#9-memory-model)
10. [Variable Scope & Lifecycle](#10-variable-scope--lifecycle)
11. [Modules & Imports](#11-modules--imports)
12. [Standard Library](#12-standard-library)
13. [File Handling](#13-file-handling)
14. [Cross-VM Messaging (Events)](#14-cross-vm-messaging-events)
15. [Compiler Optimizations](#15-compiler-optimizations)
16. [Error Handling](#16-error-handling)
17. [CLI & Entry Point](#17-cli--entry-point)

---

## 1. Introduction

Darim is designed with a clear philosophy: **simplicity of syntax, depth of semantics**. It does not rely on a host language's runtime — it compiles to its own bytecode, executed by a custom register-based VM, with its own memory allocator and garbage collector.

### Design Goals

- Predictable memory semantics (explicit heap vs stack distinction)
- Clean, readable syntax without ambiguity
- Expressive type system without sacrificing simplicity
- First-class functions and lambdas
- Robust standard library for common operations

### Architecture Overview

```
Source Code (.d files)
        │
        ▼
      Lexer
  (token stream)
        │
        ▼
      Parser
  (Pratt for expressions,
   Recursive Descent for syntax)
        │
        ▼
  Abstract Syntax Tree (AST)
        │
        ▼
   Bytecode Compiler
  (with constant folding)
        │
        ▼
  Register-based VM
  (custom heap, arena allocator,
   garbage collector)
```

Darim uses **Pratt parsing** for expression evaluation (enabling correct operator precedence via binding powers) and **recursive descent** for all other syntax constructs.

---

## 2. Getting Started

Every Darim program needs a `main` function as its entry point. Files use the `.d` extension.

```darim
# hello.d
void main(args: tuple) {
    displayLn("Hello, World!");
}
```

Run with:

```
darim run hello.d
```

Pass CLI arguments:

```
darim run hello.d foo bar
```

---

## 3. Variables & Constants

### Declaration

Darim uses two keywords for declaring variables:

- `var` — declares a mutable variable
- `final` — declares an immutable constant (value cannot be reassigned)

```darim
var x = 10;          # mutable, inferred as number
final pi = 3.14159;  # immutable constant

var name = "darim";  # inferred as string
var flag = true;     # inferred as bool
```

### Explicit Type Annotation

You can optionally annotate the type:

```darim
var count: num = 0;
var label: string = "hello";
var active: bool = false;
```

Type annotations are required in some contexts (e.g., typed function parameters, explicit array element access). See each section for details.

### Type Inference

Darim infers the type from the right-hand side at declaration time. Once inferred, the type is fixed:

```darim
var x = 5;
x = 10;      # OK, still a number
x = "text";  # ERROR: type mismatch, x is number
```

### Explicit Type Casting

Arrays are untyped, so reading a typed value from them requires an explicit cast with `as`, or a typed accessor:

```darim
var arr = new {1, 2, 3};
var n: num = arr[0] as num;   # explicit cast: array element → number
var m = arr.num(0);           # typed accessor: same cast, VM throws on failure
```

If the cast fails at runtime, an error is thrown.

> **🔮 Future Enhancement:** Explicit type declarations will be enforced more broadly for stronger compile-time guarantees and deeper optimization opportunities, including separate `int` and `float` types, unsigned numbers, and bit-level operations.

---

## 4. Data Types

### 4.1 Numbers

All numeric values in Darim are unified under a single `num` type. This covers integers and floating-point values.

```darim
var a = 42;
var b = 3.14;
var c = -7;
var d: num = 100;
```

**Arithmetic:**

```darim
var sum = 10 + 3;      # 13
var diff = 10 - 3;     # 7
var prod = 4 * 5;      # 20
var quot = 10 / 4;     # 2.5
var mod = 10 % 3;      # 1
var floor = 11 // 4;   # 2  (floor division)
var power = 2 ^ 8;     # 256
```

**Truthy / Falsy:**

`0` is falsy; all other numbers are truthy.

```darim
var x = 0;
if x: displayLn("truthy");   # not printed
if x + 1: displayLn("yes");  # printed, because 1 is truthy
```

> **🔮 Future Enhancement:** Separate `int` and `float` types, unsigned integers (`uint`), and bitwise operations (`&`, `|`, `^`, `~`, `<<`, `>>`).

---

### 4.2 Booleans

Boolean values are `true` and `false`.

```darim
var yes = true;
var no = false;
var flag: bool = true;
```

**Truthy and Falsy Values:**

The following values are **falsy** in bool context:

- `false`
- `0`
- `null`
- Empty collections: `[]`, `{}`, empty `Map`, empty `Set`

Everything else is **truthy**.

```darim
var arr = new [];
if arr: displayLn("has items");   # not printed, empty array is falsy

var map = new Map();
if !map: displayLn("empty map");  # printed

var x: bool = 0;    # x is false (implicit cast from number)
var y: bool = "hi"; # y is true (non-null string is truthy)
```

---

### 4.3 Strings

Strings are **immutable** and **non-iterable** by default.

```darim
var greeting = "Hello, World!";
var name = "Darim";
var full = greeting + " " + name;  # concatenation
```

#### Stack vs Heap Strings

By default, strings are stored on the **stack**. Use `new` to allocate on the **heap**:

```darim
var s = "short string";           # stack allocation
var large = new "heap string";    # heap allocation
```

Heap-allocated strings persist beyond their defining scope and can hold much larger content (useful for reading entire files). When you reassign a heap string, the new value is also written to the heap.

```darim
var file = new "";
file = readFile(openFile("data.txt", FileMode.Read));  # safe, heap-allocated
```

**String Methods:**

```darim
var s = "Hello, World!";

# Substring: substr(str, start, length)
var sub = substr(s, 7, 5);         # "World"

# Index search
var i = indexOf(s, "World");       # 7
var li = lastIndexOf(s, "l");      # 10

# Case
var up = ucase(s);                 # "HELLO, WORLD!"
var lo = lcase(s);                 # "hello, world!"

# Trim
var padded = "  hello  ";
var trimmed = trim(padded);        # "hello"
var lt = ltrim(padded);            # "hello  "
var rt = rtrim(padded);            # "  hello"

# Replace
var r = replace(s, "World", "Darim");     # "Hello, Darim!"
var ra = replaceAll(s, "l", "L");         # "HeLLo, WorLd!"

# Split: single arg = character array; two args = split by delimiter
var chars = strsplit(s);            # ['H','e','l','l','o',...]
var words = strsplit(s, " ");       # ["Hello,", "World!"]

# Prefix / suffix
var starts = startsWith(s, "Hello");   # true
var ends = endsWith(s, "World!");      # true

# Length
var len = length(s);               # 13

# Comparison
var eq = strEquals(s, "Hello, World!");   # true
var cmp = strcmp(s, "Hello, Darim!");     # positive number (s > arg)
```

**String Comparison:**

Use `strEquals` for equality. Use `strcmp` for ordering (returns negative, zero, or positive):

```darim
var a = "apple";
var b = "banana";

if strEquals(a, b): displayLn("equal");
if strcmp(a, b) < 0: displayLn("apple comes first");  # printed
```

> **Note:** Strings are passed by reference but are an exception to the scope rule — you can safely return a `string` from a function even though it lives on the stack. See [Memory Model](#9-memory-model) for details.

> **🔮 Future Enhancement:** Iterable strings (`for c in str:`), and a mutable `StringBuilder` type for performance-sensitive string construction.

---

### 4.4 Arrays

Darim has two kinds of arrays with distinct memory and performance characteristics.

#### Dynamic Arrays (Heap)

```darim
var arr = new [];          # empty dynamic array
var nums = new {1, 2, 3};  # initialized dynamic array
```

- Stored on the **heap**
- Elements are heap-referenced (one level of pointer indirection)
- Resizable via array methods
- Safe to return from functions

#### Fixed (Static) Arrays (Stack)

```darim
var fixed = {1, 2, 3, "hello"};  # inferred size (4 elements)
var sized = [50];                # fixed array, 50 slots, uninitialized
```

- Stored on the **stack**
- Faster access (no pointer chasing)
- Size is immutable
- **Cannot be safely returned** from functions (stack memory is freed on scope exit)

#### Indexing

Arrays are **zero-indexed**:

```darim
var arr = new {10, 20, 30, 40};

var first = arr[0];     # 10
var third = arr[2];     # 30
arr[1] = 99;            # assign at index

# Explicit type on access — arrays are untyped, so cast with `as`
var n: num = arr[0] as num;   # 10, cast to number

# Typed accessors — the VM casts and throws on failure
var m = arr.num(0);     # 10
var s = arr.string(0);  # "10"
var b = arr.bool(0);    # true
```

> **⚠ Warning:** Indexing treats the array as fixed. On a dynamic array with 1 element, `arr[1] = 5` throws an index out of bounds error. Use `push` to grow dynamic arrays.

#### Array Methods

Methods treat the array as **dynamic**. Using them on a fixed array throws an error.

```darim
var arr = new [];

# Push / PushFront
push(arr, 5);           # arr = [5]
push(arr, 10);          # arr = [5, 10]
pushFront(arr, 1);      # arr = [1, 5, 10]

# Pop — removes and returns last element
var last = pop(arr);    # last = 10, arr = [1, 5]

# Shift — removes and returns first element, shifts left
var first = shift(arr); # first = 1, arr = [5]

# Length
var len = length(arr);  # 1

# Contains check
var found = arrayContains(arr, 5);  # true
var nope  = arrayContains(arr, 99); # false
```

> **Note on `arrayContains`:** This is the preferred way to search arrays. It is type-forgiving — on type mismatch it returns `false` rather than throwing an error, and it scans the entire array regardless of mismatches.

#### Iteration

```darim
var nums = new {10, 20, 30, 40};

# Value only
for var item: num in nums: {
    display(item);
}

# Index and value
for var i, item: num in nums: {
    displayLn("index: ", i, " value: ", item);
}

# Range-based (manual index)
for var i in 0..length(nums): {
    var n: num = nums[i];
    display(n);
}
```

#### Functional Utilities

```darim
var nums = new {3, 1, 4, 1, 5, 9};

# Reverse in-place
reverse(nums);                        # {9, 5, 1, 4, 1, 3}

# Sort
sortAsc(nums);                        # {1, 1, 3, 4, 5, 9}
sortDesc(nums);                       # {9, 5, 4, 3, 1, 1}

# Sort by key (lambda)
var people = new [];
# ... populated with tuples (name, age)
sortAsc(people, (p: tuple): num { return p[1]; });  # sort ascending by age

# Map — transform each element
var doubled = map(nums, (x: num): num { return x * 2; });  # new array: each element doubled

# Reduce — fold to single value
var total = reduce(nums, (acc: num, val: num): num { return acc + val; });  # sum of all

# Length
var size = length(nums);
```

> **🔮 Future Enhancement:** `filter`, `flatMap`, lazy iteration, and typed arrays (`var arr: num[] = new []`) that eliminate runtime casting overhead.

---

### 4.5 Tuples

Tuples are **ordered, heterogeneous, immutable** collections.

```darim
var point = Tuple(10, 20);
var record = Tuple(42, "Alice", true);
var empty = Tuple();
```

- Defined with the `Tuple(...)` constructor — there is no paren-literal `(1, 2, 3)` syntax
- Immutable — elements cannot be changed after creation
- Indexed the same way as arrays (zero-based)
- Stack-allocated by default; use `new` for heap allocation

```darim
var t = Tuple(1, "hello", 3.14);

var n: num = t[0];       # 1
var s: string = t[1];    # "hello"
var f: num = t[2];       # 3.14
```

**Heap Tuples (returnable from functions):**

```darim
var t = new Tuple(1, "hello");   # heap-allocated, safe to return
```

**Multi-return (Tuples as Return Vehicles):**

See [Functions → Multi-return](#multi-value-return) for how tuples integrate with function return values.

> **🔮 Future Enhancement:** Named tuples (`var t = Tuple(x:1, y:2)`) with field access by name.

---

### 4.6 Maps

Maps are key-value stores. Keys must be `string` or `num`; values can be any type. Maps are always **heap-allocated**.

```darim
# Empty map
var m = new Map();

# Map with initial entries (key, value pairs)
var scores = new Map("alice", 95, "bob", 87, "carol", 92);
```

**Methods:**

```darim
var m = new Map("a", 1);

# Add / update entry
mapPut(m, "b", 2);
mapPut(m, "a", 99);           # update existing

# Get value
var val = mapGet(m, "a");     # 99
var safe = mapGet(m, "x", 0); # 0 (default), avoids error if key missing

# Check existence
var has = mapContains(m, "b");   # true
var miss = mapContains(m, "z");  # false

# Add multiple at once (must have even number of args)
mapPutAll(m, Tuple("c", 3, "d", 4));

# Length
var size = length(m);
```

**Iteration:**

```darim
var m = new Map("x", 10, "y", 20, "z", 30);

for var k, v in m: {
    displayLn("key: ", k, " value: ", v);
}
```

> **⚠ Note:** `mapGet` throws an error if the key doesn't exist and no default is provided. Always use the two-argument form when the key may be absent.

> **🔮 Future Enhancement:** Typed maps (`Map<string, num>`), ordered maps backed by red-black trees, and sorted iteration.

---

### 4.7 Sets

Sets are unordered collections of **unique** values. Sets are **statically typed** (all elements must share the same type) and always **heap-allocated**.

```darim
# Empty set
var s = new Set();

# Set with initial values
var primes = new Set(2, 3, 5, 7, 11);
var tags   = new Set("alpha", "beta", "gamma");
```

**Methods:**

```darim
var s = new Set(1, 2, 3);

# Add element (throws on type mismatch)
setPut(s, 4);

# Add multiple
setPutAll(s, Tuple(5, 6, 7));

# Check membership
var has = setContains(s, 3);   # true
var no  = setContains(s, 99);  # false

# Length
var size = length(s);
```

**Iteration:**

```darim
var s = new Set("a", "b", "c");

for var item in s: {
    displayLn(item);
}
```

> **⚠ Note:** `setPut` throws a type error if the inserted value doesn't match the set's inferred type.

---

### 4.8 Enums

Enums define a named set of constant values.

```darim
enum Direction { North, South, East, West }

enum Status {
    Pending = 0,
    Active  = 1,
    Closed  = 2
}
```

**Usage:**

```darim
var dir: Direction = Direction.North;

match dir {
    North { displayLn("going north"); }
    South { displayLn("going south"); }
    _ { displayLn("going sideways"); }
}
```

**Enum Methods:**

```darim
var name = enumName(Direction.North);     # "North"
var val  = fromName(Direction, "South");  # Direction.South
```

---

## 5. Operators & Expressions

### Arithmetic Operators

| Operator | Description          | Example           | Result |
|----------|----------------------|-------------------|--------|
| `+`      | Addition             | `5 + 3`           | `8`    |
| `-`      | Subtraction          | `10 - 4`          | `6`    |
| `*`      | Multiplication       | `3 * 7`           | `21`   |
| `/`      | Division             | `10 / 4`          | `2.5`  |
| `%`      | Modulo               | `10 % 3`          | `1`    |
| `//`     | Floor Division       | `11 // 4`         | `2`    |
| `^`      | Exponentiation       | `2 ^ 10`          | `1024` |

### Comparison Operators

```darim
var a = 5;
var b = 10;

a == b;    # false
a != b;    # true
a < b;     # true
a > b;     # false
a <= b;    # true
a >= b;    # false
```

### Logical Operators

```darim
var x = true;
var y = false;

x and y;   # false
x or y;    # true
not x;     # false
!x;        # false (shorthand for not)
```

### Between Operator

The `bet` operator checks if a value falls within a range (inclusive on both ends). The two range operands can be in any order:

```darim
var x = 5;

if x bet 1, 10: displayLn("in range");    # printed
if x bet 10, 1: displayLn("also works");  # also printed (order doesn't matter)
if x bet 6, 10: displayLn("out");         # not printed

# Assigns a bool result
var inRange: bool = 15 bet 10, 20;   # true
```

### Operator Precedence

Higher tiers evaluate first. Within a tier, operators are left-associative unless noted.

| Tier  | Operators                                   | Associativity |
|-------|---------------------------------------------|---------------|
| 1st   | `()` call, `[]` index, `.` member, `++` `--` postfix | left          |
| 2nd   | `!` `not` `-` `+` prefix unary, `++` `--` prefix    | —             |
| 3rd   | `as` cast                                    | —             |
| 4th   | `^` exponentiation                           | **right**     |
| 5th   | `*` `/` `%` `//`                             | left          |
| 6th   | `+` `-`                                      | left          |
| 7th   | `..` range                                   | left          |
| 8th   | `<` `>` `<=` `>=`                            | left          |
| 9th   | `==` `!=`                                    | left          |
| 10th  | `bet` (x bet lo, hi)                         | —             |
| 11th  | `&&` / `and`                                 | left          |
| 12th  | `\|\|` / `or`                                | left          |
| 13th  | `?:` ternary                                 | right         |
| 14th  | `=` `+=` `-=` `*=` `/=` `^=` `%=`            | right         |

Prefix unary binds **tighter than `^`**, so `-2 ^ 2` parses as `(-2) ^ 2` = `4` (not `-(2 ^ 2)`). Assignment is the loosest operator, so `var a = 5 >= b` gives `a` the bool result of `5 >= b`.

Use parentheses to override:

```darim
var result = 2 + 3 * 4;      # 14 (not 20)
var forced = (2 + 3) * 4;    # 20
```

### Expression Folding (Compile-time)

Darim's compiler folds constant expressions at compile time:

```darim
final TAX = 0.13;
return 1000 + 1000 * TAX;   # compiled as: return 1130.0
```

This only applies when all operands are constants or `final` variables. If any operand is `var`, folding is skipped:

```darim
var rate = 0.13;
return 1000 + 1000 * rate;  # not folded; evaluated at runtime
```

---

## 6. Control Flow

### 6.1 If / Elif / Else

```darim
var score = 75;

if score >= 90: displayLn("A");
elif score >= 75: displayLn("B");   # printed
elif score >= 60: displayLn("C");
else displayLn("F");
```

Block form (multiple statements):

```darim
var x = 10;

if x > 5: {
    displayLn("x is greater than 5");
    displayLn("x = ", x);
}
elif x == 5: {
    displayLn("x is exactly 5");
}
else {
    displayLn("x is less than 5");
}
```

Conditions can use any truthy/falsy value:

```darim
var arr = new {1, 2, 3};
if arr: displayLn("array has elements");  # truthy: non-empty

var empty = new [];
if !empty: displayLn("empty array");       # falsy: empty
```

---

### 6.2 For Loops

#### Range-based Loop

The range `a..b` is **exclusive** on the upper bound (iterates `a` to `b-1`):

```darim
for var i in 1..5: display(i);      # prints: 1 2 3 4
for var i in 10..1: display(i);     # prints: 10 9 8 ... 2 (descending)
```

#### Collection Iteration

```darim
var fruits = new {"apple", "banana", "cherry"};

# Value only
for var item: string in fruits: {
    displayLn(item);
}

# Index and value
for var i, item: string in fruits: {
    displayLn(i, ": ", item);
}
```

#### Break

```darim
for var i in 1..100: {
    if i == 5: break;
    display(i);    # prints 1 2 3 4
}
```

#### Nested Loops

```darim
for var i in 1..4: {
    for var j in 1..4: {
        display(i * j, " ");
    }
    displayLn("");
}
```

---

### 6.3 While Loops

```darim
var i = 0;
while i < 5: {
    displayLn(i);
    i = i + 1;
}

# Post-increment in condition
var x = 1;
while (x++ < 5): display(x);   # prints 2 3 4 5
```

While loops also support `break`:

```darim
var n = 0;
while true: {
    n = n + 1;
    if n == 10: break;
}
displayLn(n);  # 10
```

---

### 6.4 Match Expression

`match` is an expression that compares a value against patterns and runs the first arm whose pattern matches. `_` is the default arm. Arms are written `pattern { block }` — there is no `case`/`default` keyword.

#### Basic Match

```darim
var day = "Monday";

match day {
    "Saturday" { displayLn("weekend"); }
    "Sunday"   { displayLn("weekend"); }
    _          { displayLn("weekday"); }    # printed
}
```

#### Match as Expression

The match expression can return a value — the matched arm's block ends with the value it produces:

```darim
var op = "+";
var a = 10;
var b = 3;

var result = match op {
    "+" { a + b; }
    "-" { a - b; }
    "*" { a * b; }
    "/" { a / b; }
    _   { 0; }
};
displayLn(result);  # 13
```

#### Enum Match

```darim
enum Season { Spring, Summer, Autumn, Winter }

var s: Season = Season.Winter;

match s {
    Spring { displayLn("flowers"); }
    Summer { displayLn("sun"); }
    Autumn { displayLn("leaves"); }
    Winter { displayLn("snow"); }    # printed
}
```

> **🔮 Future Enhancement:** Structural pattern matching (`match x { (a, b) { ... } }`) for destructuring tuples and other compound values.

---

## 7. Functions

Functions must declare their return type. Parameters must be typed.

### Basic Functions

```darim
# No return value
void greet(name: string) {
    displayLn("Hello, ", name);
}

# Returns a value
num square(x: num) {
    return x * x;
}

# Multiple parameters
num add(a: num, b: num) {
    return a + b;
}
```

### Default Arguments

```darim
string formatName(first: string, last: string = "Unknown") {
    return first + " " + last;
}

formatName("Alice", "Smith");   # "Alice Smith"
formatName("Bob");              # "Bob Unknown"
```

Default arguments must be constants or literals. They are resolved at compile time (the compiler inserts the default value at each call site that omits the argument).

### Variadic Functions

Variadic arguments are collected into a **tuple**:

```darim
num addAll(nums*: num) {
    return reduce(nums, (a: num, b: num): num { return a + b; });
}

var total = addAll(1, 2, 3, 4, 5);   # 15
```

The compiler wraps the variable arguments into a tuple automatically at each call site.

### Multi-value Return

Functions can return more than one value:

```darim
num, string describe(x: num) {
    if x > 0: return x, "positive";
    elif x < 0: return x, "negative";
    else return 0, "zero";
}
```

**Calling multi-return functions:**

```darim
var n, label = describe(42);    # n=42, label="positive"
var _, label = describe(-5);    # ignore first value
var n = describe(10);           # only first value captured
```

Use `_` as a placeholder to skip return values you don't need:

```darim
num, string, bool complexResult() {
    return 42, "hello", true;
}

var _, s, _ = complexResult();   # only the string
var a, b, c = complexResult();   # all three
var a       = complexResult();   # only the first
```

### Pass by Value vs Reference

Understanding this is critical for avoiding bugs.

| Type                              | Passed By   |
|-----------------------------------|-------------|
| `num`, `bool`                     | **Value**   |
| `string`                          | Reference   |
| `tuple` (stack)                   | Reference   |
| `tuple` (heap, `new`)             | Reference   |
| Fixed array                       | Reference   |
| Dynamic array (`new []`)          | Reference   |
| `Map`, `Set`                      | Reference   |

Numbers and booleans are **copied** on every function call — mutations inside the function do not affect the caller's variable. All other types share the same underlying memory.

```darim
void double(x: num) {
    x = x * 2;         # does NOT affect caller's variable
}

void appendItem(arr: array, item: num) {
    push(arr, item);   # DOES affect caller's array (shared reference)
}

var n = 5;
double(n);
displayLn(n);   # still 5

var list = new {1, 2};
appendItem(list, 3);
displayLn(length(list));  # 3 — list was mutated
```

---

## 8. Lambdas

Lambdas are anonymous functions and are **first-class citizens** in Darim. They can be stored in variables, passed as arguments, and returned from functions.

### Syntax

A lambda is a typed parameter list followed by a `: returnType` and a block. Unnamed lambdas are passed directly to a call; a `final` declaration names one (a lambda reference never changes, so `final` fits):

```darim
# Named lambda
final add = (a: num, b: num): num { return a + b; };

# Unnamed lambda passed directly to a call
testCallback((a: num, b: string): bool { return a == 1; });
```

The parser recognizes a bare `(` followed by typed parameters and `: returnType` as a lambda, not a grouping. When passing an existing lambda, just name it:

```darim
testCallback(add);
```

### Lambda Function Types

The type of a function or lambda uses the `func` keyword — parameter types, then return type after the colon:

```darim
func(num, num): num            # two params, one return
func(): void                   # no params, no return
func(): string                 # no params, returns string
func(num, string): num, string # returns multiple values
```

Typed annotation for a lambda variable:

```darim
final fun: func(num, string): num = (a: num, b: string): num {
    displayLn("number: ", a);
    return a * 2;
};
```

### Lambdas as Arguments

```darim
var nums = new {5, 3, 8, 1, 9};

sortAsc(nums, (x: num): num { return x; });           # sort by identity
var doubled = map(nums, (x: num): num { return x * 2; });
var sum = reduce(nums, (a: num, b: num): num { return a + b; });
```

### Lambdas as Return Values

```darim
func(num): num makeMultiplier(factor: num) {
    final f = factor;
    final fn = (x: num): num { return x * f; };
    return fn;
}

var triple = makeMultiplier(3);
displayLn(triple(5));   # 15
```

### Scope and Variable Capture

> **⚠ Important:** Lambdas in Darim do **not** form closures. A lambda can read globals and any variable still in scope where it runs (like Java). No variable is kept alive by the lambda — except a `final` constant, which can be captured safely because capturing it copies its value from one stack frame to another.

```darim
func(num): num makeAdder() {
    var base = 10;
    final fn = (x: num): num { return base + x; };

    var r = fn(5);      # works: base is still in scope here → 15
    return fn;          # fn is returned; base is NOT captured (it's a var)
}

void main(args: tuple) {
    var adder = makeAdder();
    adder(5);           # ERROR: base is out of scope and was freed
}
```

**Best practices for lambdas:**

- Use lambdas as simple callbacks (`map`, `reduce`, `sort`)
- Pass needed values as lambda parameters rather than relying on outer scope
- If you must use outer scope variables, ensure they are on the heap (`new`, `Map`, `Set`, dynamic arrays) and will outlive the lambda's execution
- Global variables are in scope for lambdas but global state is generally discouraged

```darim
# SAFE: value passed as argument
var nums = new {1, 2, 3};
var factor = 3;
var scaled = map(nums, (x: num): num { return x * factor; });  # factor still in scope at the call

# SAFE: a `final` constant is captured by copying its value
func(num): num makeMultiplier(factor: num) {
    final f = factor;
    return (x: num): num { return x * f; };  # f's value is copied into the lambda's frame
}

# UNSAFE: returning a lambda that references a stack `var`
func(): num badAdder() {
    var x = 5;
    return (): num { return x + 1; };  # x is freed when badAdder exits
}
```

> **🔮 Future Enhancement:** Full closure support with heap lifting of captured variables. This requires the compiler to identify closed-over variables and move them to the heap automatically.

---

## 9. Memory Model

Darim manages memory explicitly, without relying on the host language's GC. Understanding where values live is key to writing correct programs.

### Stack vs Heap

| Type                         | Location  | Notes                                  |
|------------------------------|-----------|----------------------------------------|
| `num`, `bool`                | Stack     | Pass by value; safe to return          |
| `string` (no `new`)          | Stack     | **Exception:** safe to return          |
| `string` (with `new`)        | Heap      | Safe to return, survives scope         |
| `tuple` (no `new`)           | Stack     | **Unsafe** to return; returns null     |
| `tuple` (with `new`)         | Heap      | Safe to return                         |
| Fixed array (`{...}`)        | Stack     | **Unsafe** to return; returns null     |
| Dynamic array (`new []`)     | Heap      | Safe to return                         |
| `Map`                        | Heap      | Always safe to return                  |
| `Set`                        | Heap      | Always safe to return                  |

### Return Safety

A common source of bugs is returning stack-allocated reference types. Their memory is freed when the function exits:

```darim
# UNSAFE: fixed array returned by reference — memory freed on exit
{num} badArray() {
    var arr = {1, 2, 3};
    return arr;    # returns a dangling pointer → null
}

# SAFE: dynamic array on heap
array goodArray() {
    var arr = new {1, 2, 3};
    return arr;    # heap memory persists
}

# SAFE: numbers are value types
num safeNumber() {
    var x = 42;
    return x;      # copied, safe
}

# SAFE: strings are the exception
string safeString() {
    var s = "hello";
    return s;      # safe despite being on stack
}

# UNSAFE: stack tuple
tuple badTuple() {
    var t = Tuple(1, 2, 3);
    return t;      # null on caller side
}

# SAFE: heap tuple
tuple goodTuple() {
    var t = new Tuple(1, 2, 3);
    return t;      # safe
}
```

### Garbage Collector

Darim uses a custom garbage collector that operates on heap memory. Heap objects are tracked via reference counting or mark-and-sweep (implementation-dependent by version). The GC reclaims memory when objects are no longer reachable.

Stack memory is freed deterministically at scope exit — no GC involvement.

> **🔮 Future Enhancement:** Arena allocation for contiguous, fast memory regions. Custom mark-and-sweep GC with explicit reference tracking. Native memory mapping via C interop (Java Panama) to bypass the JVM heap entirely.

---

## 10. Variable Scope & Lifecycle

Variables in Darim are **block-scoped**. A variable is accessible from the point of its declaration to the end of the enclosing block `{}`.

### Lexical Scoping

```darim
var x = 4;

void outer() {
    var y = 10;

    void inner() {
        displayLn(x);   # 4 — found in outer file scope
        displayLn(y);   # 10 — found in outer()
    }
    inner();
}

outer();
```

When a name is referenced, Darim searches from the innermost scope outward:

```darim
var x = 1;

void test() {
    var x = 2;        # shadows the outer x
    displayLn(x);     # 2
}

test();
displayLn(x);         # 1
```

### Scope in Loops and Blocks

```darim
for var i in 1..5: {
    var temp = i * 2;
    displayLn(temp);
}
# temp is not accessible here — out of scope

var total = 0;
for var i in 1..6: {
    total = total + i;  # total is accessible: declared outside the loop
}
displayLn(total);   # 15
```

---

## 11. Modules & Imports

Darim programs can be split across multiple `.d` files.

### Visibility

By default, all functions and variables are **private** to the file they are declared in. `visible` is a separate statement that marks an already-declared symbol as exported:

```darim
# math_utils.d

var PI = 3.14159;

num circleArea(r: num) {
    return PI * r ^ 2;
}
visible circleArea;

num circumference(r: num) {
    return 2 * PI * r;
}
visible circumference;

# PI is not visible — use a getter to expose it
num getPI() { return PI; }
visible getPI;
```

### Including Modules

```darim
# main.d
include math_utils as mu;

var area = mu.circleArea(5.0);
displayLn(area);

displayLn(mu.getPI());   # 3.14159
```

The `include` keyword binds all `visible` symbols of the file to the given namespace. Both `include` and `import` always require an `as <alias>` — there is no unaliased form.

### Setter Pattern

`visible` variables appear as **constants** to importing files. To allow modification, expose a setter function:

```darim
# config.d
var threshold = 0.5;

num getThreshold() { return threshold; }
visible getThreshold;

void setThreshold(v: num) { threshold = v; }
visible setThreshold;
```

```darim
# main.d
include config as cfg;

displayLn(cfg.getThreshold());   # 0.5
cfg.setThreshold(0.75);
displayLn(cfg.getThreshold());   # 0.75
```

All files must be passed to the compiler explicitly, in leaf-first order (dependencies before the files that use them). The compiler processes left to right — if a file is referenced but wasn't passed, it's a compile error. No automatic file searching.
```darimc utils.d helpers.d main.d --out program.dbc```

### Built-in Math Module

```darim
import Math as math;

math.ceil(4.3);       # 5
math.floor(4.7);      # 4
math.abs(-9);         # 9
math.sqrt(16);        # 4.0
math.random();        # float between 0.0 and 1.0
math.random(1, 10);   # integer between 1 and 10
```

> **🔮 Future Enhancement:** Selective imports (`include math { add, sub }`), a package system with folder-based module resolution, and a dependency resolution mechanism.

---

## 12. Standard Library

### Display & Input

```darim
display("value: ", x);       # print without newline
displayLn("done");           # print with newline

var word = read("Enter word: ");       # reads until whitespace
var line = readln("Enter line: ");     # reads until newline
```

### Collection Utilities

```darim
length(collection);              # number of elements

reverse(arr);                    # in-place reversal (void)
sortAsc(arr);                    # in-place ascending sort
sortDesc(arr);                   # in-place descending sort
sortAsc(arr, (item: tuple): num { return item[1]; });   # sort by key (lambda)
sortDesc(arr, (item: tuple): num { return item[1]; });  # descending by key

map(arr, (x: num): num { return x * 2; });            # returns new transformed array
reduce(arr, (a: num, b: num): num { return a + b; }); # folds to single value
```

### Math

```darim
import Math as math;

math.ceil(x);
math.floor(x);
math.abs(x);
math.sqrt(x);
math.random();         # [0.0, 1.0)
math.random(a, b);     # integer in [a, b]
```

---

## 13. File Handling

Darim supports text-mode file I/O. Files are treated as Unicode text.

### Opening Files

```darim
var f = openFile("data.txt", FileMode.Read);
var w = openFile("out.txt", FileMode.Write);
var a = openFile("log.txt", FileMode.Append);
```

### Reading

```darim
# Read entire file as a string (use heap string for large files)
var content = new "";
content = readFile(f);

# Read line by line
while !isEof(readLine(f)): {
    var line = readLine(f);
    displayLn(line);
}
```

### Writing

```darim
var f = openFile("output.txt", FileMode.Write);
writeFile(f, "Hello, file!");
writeLine(f, "This writes a line with newline");
```

### Practical Example

```darim
void main(args: tuple) {
    var inputPath: string = args[0];
    var file = openFile(inputPath, FileMode.Read);

    var lineCount = 0;
    while !isEof(readLine(file)): {
        var line = readLine(file);
        lineCount = lineCount + 1;
        displayLn(lineCount, ": ", line);
    }
    displayLn("Total lines: ", lineCount);
}
```

---

## 14. Cross-VM Messaging (Events)

Darim programs can communicate with external applications (e.g., a web server) through an event system. Data is passed as text.

### Emitting Events

```darim
# Send an event named "response" with a string payload
emitEvent("response", "status:ok");
```

### Listening for Events

```darim
# Register a handler for events named "request"
onEvent("request", (data: string): string {
    displayLn("received: ", data);
    return "acknowledged";   # return a string response, or 0 for void
});
```

The handler receives the event payload as a string and **must return** either:
- A `string` — sent back as the response
- `0` (number) — indicates no response (void)

### Full Example

```darim
void main(args: tuple) {
    displayLn("Server started");

    onEvent("compute", (data: string): num {
        var n: num = data as num;   # cast from string to number
        var result = n * n;
        return result;              # returned as response
    });

    onEvent("shutdown", (data: string): num {
        displayLn("Shutting down");
        return 0;
    });
}
```

---

## 15. Compiler Optimizations

### Constant Folding

The compiler evaluates pure constant expressions at compile time and replaces them with the result. This applies when all operands are `final` variables or numeric/string literals:

```darim
final WIDTH  = 800;
final HEIGHT = 600;
final AREA   = WIDTH * HEIGHT;   # compiled as: 480000

# Used in expressions:
return WIDTH / 2 + 5;   # compiled as: 405
```

If any operand is a `var`, the expression is evaluated at runtime:

```darim
var width = 800;
return width / 2 + 5;   # NOT folded; computed at runtime
```

**Nested folding:**

```darim
final A = 2;
final B = 3;
final C = A ^ B + 10 / 2;    # 2^3 + 5 = 13 — folded at compile time
```

> **🔮 Future Enhancements:**
> - **Dead code elimination** — unreachable branches after constant folding are removed
> - **Inline expansion** — small functions inlined at call sites to eliminate call overhead
> - **Register allocation optimization** — smarter assignment of values to VM registers for fewer memory round-trips

---

## 16. Error Handling

### Compile-time Errors

The compiler catches:

- Type mismatches in assignments and function calls
- Undefined variables or functions
- Index out of bounds (for known-size fixed arrays)
- Mismatched return types
- Invalid use of `visible` or `include`

```darim
var x: num = "hello";   # ERROR: cannot assign string to num
var y = undefinedVar;   # ERROR: undefinedVar not declared

num add(a: num, b: num) {
    return "result";    # ERROR: return type mismatch
}
```

### Runtime Errors

Some errors can only be detected at runtime:

```darim
# Index out of bounds
var arr = new {1, 2, 3};
arr[5] = 99;              # RUNTIME ERROR: index 5 out of bounds (size 3)

# Type cast failure
var mixed = new {1, "hello", 3};
var n = mixed.num(1);     # RUNTIME ERROR: cannot cast "hello" to num

# Map key not found
var m = new Map("a", 1);
mapGet(m, "z");           # RUNTIME ERROR: key "z" not found (use default form)

# Set type mismatch
var s = new Set(1, 2, 3);
setPut(s, "text");        # RUNTIME ERROR: type mismatch in set

# Null dereference (stack value returned from function)
{num} bad() {
    var arr = {1, 2, 3};
    return arr;
}
var a = bad();
var x = a.num(0);         # RUNTIME ERROR: null dereference
```

### Null Safety

Returning stack-allocated reference types results in `null` at the call site. Darim does not prevent this at compile time in Version 1 — it is the programmer's responsibility. Following the [return safety rules](#return-safety) in the Memory Model section prevents these issues.

```darim
# Safe pattern: check before use
var result = someFunctionThatMightReturnNull();
if result: {
    # proceed safely
}
```

> **🔮 Future Enhancement:** Structured `try / catch` exception handling:
> ```darim
> try {
>     var n = mapGet(m, "key");
>     displayLn(n);
> }
> catch e {
>     displayLn("Error: ", e);
> }
> ```

---

## 17. CLI & Entry Point

### The `main` Function

Every Darim program must define a `main` function. It is the single entry point:

```darim
void main(args: tuple) {
    displayLn("Program started");
}
```

`args` is a tuple of strings — the command-line arguments passed at runtime. `args[0]` is the first argument.

### CLI Argument Examples

```darim
# run with: darim run app.d Alice 30

void main(args: tuple) {
    var name: string = args[0];    # "Alice"
    var age: string  = args[1];    # "30"

    displayLn("Name: ", name);
    displayLn("Age:  ", age);
}
```

Arguments are always received as strings. Cast explicitly if needed:

```darim
void main(args: tuple) {
    var count: num = args[0] as num;   # cast string → num
    for var i in 1..count + 1: {
        displayLn(i);
    }
}
```

### Multi-file Programs

```darim
# utils.d
string repeat(s: string, n: num) {
    var result = "";
    for var i in 1..n + 1: {
        result = result + s;
    }
    return result;
}
visible repeat;

# main.d
include utils as u;

void main(args: tuple) {
    displayLn(u.repeat("ab", 3));   # "ababab"
}
```

---

## Quick Reference

### Keywords

| Keyword   | Purpose                                  |
|-----------|------------------------------------------|
| `var`     | Declare mutable variable                 |
| `final`   | Declare immutable constant               |
| `void`    | Function returns nothing                 |
| `num`     | Number type                              |
| `string`  | String type                              |
| `bool`    | Boolean type                             |
| `enum`    | Define enumeration                       |
| `if`      | Conditional                              |
| `elif`    | Else-if branch                           |
| `else`    | Default branch                           |
| `for`     | For loop                                 |
| `while`   | While loop                               |
| `break`   | Exit current loop                        |
| `match`   | Match expression                         |
| `return`  | Return value from function               |
| `new`     | Heap-allocate a value                    |
| `include` | Include another file — requires `as`     |
| `visible` | Mark a declared symbol as exported       |
| `import`  | Import a built-in module — requires `as <alias>` (e.g. `import Math as math`) |
| `as`      | Cast operator / alias for include/import |
| `in`      | Loop iteration / range                   |
| `bet`     | Between operator                         |
| `and`     | Logical AND                              |
| `or`      | Logical OR                               |
| `not`     | Logical NOT                              |
| `func`    | Function type keyword                    |

### Built-in Functions at a Glance

| Function                      | Description                              |
|-------------------------------|------------------------------------------|
| `display(args...)`            | Print to stdout (no newline)             |
| `displayLn(args...)`          | Print to stdout (with newline)           |
| `read(prompt)`                | Read one word from stdin                 |
| `readln(prompt)`              | Read one line from stdin                 |
| `length(col)`                 | Length of any collection or string       |
| `reverse(col)`                | Reverse collection in-place              |
| `sortAsc(col)`                | Sort ascending in-place                  |
| `sortDesc(col)`               | Sort descending in-place                 |
| `sortAsc(col, fn)`            | Sort ascending by key function           |
| `map(col, fn)`                | Transform collection elements            |
| `reduce(col, fn)`             | Fold collection to single value          |
| `push(arr, v)`                | Append to dynamic array                  |
| `pushFront(arr, v)`           | Prepend to dynamic array                 |
| `pop(arr)`                    | Remove and return last element           |
| `shift(arr)`                  | Remove and return first element          |
| `arrayContains(arr, v)`       | Check if value exists in array           |
| `mapPut(m, k, v)`             | Set key-value in map                     |
| `mapGet(m, k)`                | Get value by key (throws if missing)     |
| `mapGet(m, k, default)`       | Get value by key with fallback           |
| `mapContains(m, k)`           | Check if key exists in map              |
| `mapPutAll(m, tuple)`         | Bulk-insert k/v pairs into map           |
| `setPut(s, v)`                | Add value to set                         |
| `setContains(s, v)`           | Check if value exists in set             |
| `setPutAll(s, tuple)`         | Bulk-insert values into set              |
| `enumName(e)`                 | Get enum value as string                 |
| `fromName(EnumType, name)`    | Get enum value from string               |
| `strEquals(a, b)`             | String equality                          |
| `strcmp(a, b)`                | String comparison (neg/zero/pos)         |
| `substr(s, start, len)`       | Substring extraction                     |
| `indexOf(s, sub)`             | First index of substring                 |
| `lastIndexOf(s, sub)`         | Last index of substring                  |
| `replace(s, from, to)`        | Replace first occurrence                 |
| `replaceAll(s, from, to)`     | Replace all occurrences                  |
| `strsplit(s)`                 | Split string into character array        |
| `strsplit(s, delim)`          | Split string by delimiter                |
| `trim(s)`                     | Remove leading and trailing whitespace   |
| `ltrim(s)`                    | Remove leading whitespace                |
| `rtrim(s)`                    | Remove trailing whitespace               |
| `startsWith(s, prefix)`       | Check string prefix                      |
| `endsWith(s, suffix)`         | Check string suffix                      |
| `ucase(s)`                    | Uppercase string                         |
| `lcase(s)`                    | Lowercase string                         |
| `openFile(path, mode)`        | Open a file                              |
| `readFile(f)`                 | Read entire file as string               |
| `readLine(f)`                 | Read next line from file                 |
| `writeFile(f, data)`          | Write data to file                       |
| `writeLine(f, data)`          | Write line with newline to file          |
| `isEof(line)`                 | Check if file is at end                  |
| `emitEvent(name, data)`       | Emit a cross-VM event                    |
| `onEvent(name, handler)`      | Register a cross-VM event handler        |

---

*Darim Language Reference — Version 1.0*
