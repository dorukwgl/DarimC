# DarimC — Compiler Roadmap

Pipeline: **Source → Lexer → Parser → AST → Bytecode Compiler → VM**

---

## Phase 0 — Project Setup

- [ ] Create project structure: `lexer/`, `parser/`, `ast/`, `compiler/`, `vm/`, `runtime/`
- [ ] Implement `main` entry point — reads source file, wires pipeline stubs
- [ ] CLI argument parsing
  - `--readable` → emit human-readable text bytecode (`.dba`)
  - `--out <file>` → output file path
  - `--dump-tokens` → print token stream and exit
  - `--dump-ast` → print AST and exit
  - `--dump-bytecode` → print bytecode listing

---

## Phase 1 — Lexer

- [ ] Define all token types (literals, keywords, operators, delimiters)
- [ ] Implement `Token` struct — type, lexeme, line, column
  > Always store line and column. Error messages without source location are useless.
- [ ] Scan single-character tokens
- [ ] Scan two-character tokens (`==`, `!=`, `<=`, `>=`, `+=`, `-=`, `*=`, `/=`, `%=`, `^=`, `++`, `--`, `&&`, `||`, `//`, `..`)
- [ ] Scan number literals (integer and float)
- [ ] Scan string literals with `\"` escape handling
- [ ] Scan identifiers and keywords (identifier → check keyword table)
- [ ] Skip whitespace and `#` line comments
- [ ] Handle newlines as implicit statement terminators
  > Only emit a newline token after tokens that can end a statement (identifiers, literals, `)`, `}`, `break`, `return`). Swallow all others as whitespace.
- [ ] Emit `TOKEN_ERROR` for unrecognized characters (don't throw — let the parser report it)

**Test:** run `darimc file.d --dump-tokens`, verify every token has correct type, lexeme, line, col.

---

## Phase 2 — Parser & AST

### 2.1 AST Nodes

- [ ] Define expression nodes: literals, identifier, binary, unary, call, index, member access, assign, lambda, bet, match-expr, cast, ternary, range
- [ ] Define statement nodes: var/final decl, block, if/elif/else, for-range, for-in, while, break, return, function decl, enum decl, include, import
- [ ] `Program` node is the per-file root: `List<Stmt> statements` + `List<Token> exports` (names from `visible <name>;`). `visible` is **not** a statement node — the parser collects the name into `Program.exports`
  > Each source file parses to its own `Program`. Passes run over all programs together: parse → typed AST → syntax analysis → final semantics analysis, which rejects calls to a symbol in another program that isn't in that program's `exports`.
- [ ] Every node stores line/column from its leading token

### 2.2 Parser Infrastructure

- [ ] `peek()`, `advance()`, `expect()`, `match()` helpers
- [ ] Error recovery — on parse error, synchronize to next statement boundary and continue (report multiple errors per run, don't abort at first)

### 2.3 Pratt Expression Parser

- [ ] Core loop: prefix handler table + infix handler table + binding powers
- [ ] Prefix handlers: literals, identifiers, unary `!`/`not`/`-`, grouping `()`, `new`; lambda detection — `(` + typed params + `: returnType` starts a lambda
- [ ] Infix handlers: binary arithmetic and comparison ops, `&&`/`and`, `||`/`or`, call `()`, index `[]`, member `.`, assign `=`, compound assigns, `as`, `..`, `?:`, `++`/`--`, typed accessors
- [ ] `bet` operator (three operands: pivot, lo, hi — comma-separated)
- [ ] `^` as right-associative
- [ ] Precedence table (loosest → tightest; right-associative where noted)

  | Tightness | Operators                                           | Assoc |
  |-----------|-----------------------------------------------------|-------|
  | loosest   | `=` `+=` `-=` `*=` `/=` `^=` `%=`                   | right |
  |           | `?`                                                 | right |
  |           | `\|\|` `or`                                         | left  |
  |           | `&&` `and`                                          | left  |
  |           | `bet` (x bet lo, hi)                                | —     |
  |           | `==` `!=`                                           | left  |
  |           | `<` `>` `<=` `>=`                                   | left  |
  |           | `..`                                                | left  |
  |           | `+` `-`                                             | left  |
  |           | `*` `/` `%` `//`                                    | left  |
  |           | `^`                                                 | right |
  |           | `as`                                                | —     |
  |           | `!` `not` `-` `+` prefix, `++` `--` prefix          | —     |
  | tightest  | `()` `[]` `.` postfix, `++` `--` postfix            | left  |

  Prefix unary binds tighter than `^`, so `-2 ^ 2` parses as `(-2) ^ 2` = `4`. Assignment is the loosest, so `var a = 5 >= b` gives `a` the bool result of `5 >= b`.

### 2.4 Statement Parsers (Recursive Descent)

- [ ] `var` / `final` declarations with optional type annotation and initializer
- [ ] Block `{ }` — list of statements separated by newlines
- [ ] `if` / `elif` / `else` — single-line and block body forms
- [ ] `for` range loop (`var i in low..high`)
- [ ] `for` collection loop (`var item in arr`, `var i, item in arr`)
- [ ] `while` loop
- [ ] `break`
- [ ] `return` — single and multi-value (`return a, b`)
- [ ] `match` expression — pattern arms with `{ block }` bodies, `_` as the default arm
- [ ] Function declarations — return types, typed params, default params, variadic params
- [ ] Enum declarations
- [ ] `include` / `import` — `import` always requires `as <alias>`
- [ ] `visible` as a separate statement — `visible myName;` marks an already-declared symbol as exported

**Test:** run `darimc file.d --dump-ast`, visually verify tree shape matches source.

---

## Phase 3 — Bytecode Design

> Decide the instruction set and encoding format here — before writing the emitter. Changing the format mid-way is expensive.

- [ ] Define register model (fixed register file per call frame, e.g. r0–r255)
- [ ] Define instruction categories: load/move, arithmetic, comparison, logical, jump, call/return, heap allocation, collection ops, string ops, I/O
- [ ] Define how constants are stored (constant pool per chunk, indexed by instructions)
- [ ] Define `Chunk` structure: instructions, constant pool, line info, name, arity
- [ ] Decide binary encoding (opcode width, operand widths, file header/magic bytes)
- [ ] Define readable text format (`.dba`) — one instruction per line, e.g. `ADD r2, r0, r1`
- [ ] Implement `BinaryWriter` and `TextWriter` — both implement the same `ChunkWriter` interface, selected at compile time based on `--readable`

> Line info (parallel array of source line per instruction offset) is essential for runtime error messages. Don't skip it.

---

## Phase 4 — Bytecode Compiler

### 4.1 Infrastructure

- [ ] Compiler state: current chunk, scope stack, symbol table (name → register)
- [ ] Register allocator: `alloc()` and `free(reg)` — linear scan is fine for v1
- [ ] Scope push/pop — free registers on scope exit
- [ ] Jump patching helpers: emit placeholder jump, backfill offset once target is known

### 4.2 Expressions

- [ ] Number, string, bool, null literals → load into register
- [ ] Variable access → resolve name to register
- [ ] Arithmetic binary ops
- [ ] Comparison and logical ops
- [ ] `bet` operator
- [ ] Assignment
- [ ] Index access and assignment (`arr[i]`, `arr[i] = v`); typed accessors `arr.num(i)` / `arr.bool(i)` / `arr.string(i)` — VM casts, throws on failure
- [ ] Member access (`map.key`, `obj.field`)
- [ ] Function call — positional args, multi-return capture, `_` discard
- [ ] `new` — dynamic array, heap tuple, heap string, map, set
- [ ] Tuple construction — `Tuple(a, b, c)` (stack), `new Tuple(a, b, c)` (heap); no paren-literal `(1, 2, 3)` syntax

### 4.3 Statements

- [ ] Variable declaration
- [ ] If / elif / else — jump patching for branches
- [ ] While loop — loop-top label, condition check, back-jump
- [ ] For range loop — init, condition, increment, back-jump
- [ ] For-in loop — index tracking, length check, element load
- [ ] Break — emit forward jump, patch to loop exit
  > Use a stack of break-patch locations per loop. On loop exit, patch them all.
- [ ] Return — single and multi-value (place results in r0, r1, …)
- [ ] Match expression — pattern arms compile to equality checks with a jump-chain; result lands in a register

### 4.4 Functions

- [ ] Each function compiles to its own `Chunk`
- [ ] Parameters occupy the first N registers (r0, r1, …)
- [ ] Default arguments — compiler inserts constant at each call site that omits the arg
- [ ] Variadic params — compiler wraps trailing args into a tuple before the call
- [ ] Lambdas compile to named anonymous chunks; a `LOAD_FUNC` instruction loads the reference
  > Syntax: `(a: num, b: num): num { ... }` — inline (unnamed) or assigned via `final name = ...`. Function types use the `func` keyword: `func(num, num): num`, `func(): void`, `func(num, string): num, string`.
- [ ] Lambda capture — no closures are formed. A lambda can read globals and any variable still in scope where it runs (like Java). A `final` constant can be captured by copying its value into the lambda's frame — the only safe way for a lambda to outlive its scope.

### 4.5 Types and Structures

- [ ] Enums — resolve member values at compile time, emit as constants. No runtime overhead.
- [ ] Static (fixed) arrays — compile like dynamic arrays but flag them as fixed-size; runtime enforces no push/pop
- [ ] Modules (`include`) — compile the included file, register its `visible` symbols under the alias namespace; calls become named calls into that chunk set
- [ ] Built-in runtime calls (`push`, `pop`, `length`, `sort`, etc.) — compiler recognizes these by name and maps them to dedicated instructions or runtime dispatch

### 4.6 Constant Folding

- [ ] AST-level pass before emission — fold binary/unary expressions whose operands are all literals or `final` variables
- [ ] Inline `final` variables that hold a constant value
- [ ] Don't fold `var` expressions — value may change at runtime

**Test:** compile with `--dump-bytecode` at each step. For the text format, read the `.dba` output and verify instruction sequence by eye.

---

## Phase 5 — Standard Library & Runtime Bindings

- [ ] `display`, `displayLn`, `read`, `readln`
- [ ] Collection: `length`, `reverse`, `sortAsc`, `sortDesc`, `map`, `reduce`
- [ ] Array methods: `push`, `pushFront`, `pop`, `shift`, `arrayContains`
- [ ] Map methods: `mapPut`, `mapGet`, `mapContains`, `mapPutAll`
- [ ] Set methods: `setPut`, `setContains`, `setPutAll`
- [ ] String methods: `substr`, `indexOf`, `lastIndexOf`, `replace`, `replaceAll`, `strsplit`, `trim`, `ltrim`, `rtrim`, `startsWith`, `endsWith`, `ucase`, `lcase`, `strcmp`, `strEquals`
- [ ] Enum helpers: `enumName`, `fromName`
- [ ] Math module: `ceil`, `floor`, `abs`, `sqrt`, `random`
- [ ] File I/O: `openFile`, `readFile`, `readLine`, `writeFile`, `writeLine`, `isEof`
- [ ] Event system: `emitEvent`, `onEvent`

## Phase 5.5 — Multi-file Compilation

 - [ ] Track all source files passed as CLI args, process in order
 - [ ] After compiling each file, register its `visible` symbols in a global symbol table keyed by filename (without extension)
 - [ ] On `include MyFile as fl` — look up `MyFile` in global symbol table; compile error if not found (wrong order or not passed)
 - [ ] Bind visible symbols under the alias `fl` in the current file's scope
 - [ ] Visible variables are inlined as constants at the import site — no setter needed to read, expose a setter function to mutate
 - [ ] `import Math as math` — maps `math.*` calls to built-in runtime functions; no file lookup needed

---

## Phase 6 — Error Reporting

- [ ] Lexer errors — unrecognized character with line/col
- [ ] Parse errors — expected token message, source line, caret pointing at position
- [ ] Compile errors — type mismatch, undeclared variable, return type mismatch, unsafe return of stack type
- [ ] Collect and report all errors before aborting — don't stop at first error
- [ ] Runtime errors — index out of bounds, key not found, type cast failure, null dereference — all include source line from chunk line info
