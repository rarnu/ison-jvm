# ISON-JVM

A minimal, token-efficient data format optimized for LLMs and Agentic AI workflows.

The origin project and why ([HERE](https://github.com/ISON-format/ison))

## Installation

### Kotlin/Java

#### with Gradle

```groovy
implementation("com.github.isyscore:ison-jvm:1.0.0.0")
```

#### with Maven

```xml
<dependency>
    <groupId>com.github.isyscore</groupId>
    <artifactId>ison-jvm</artifactId>
    <version>1.0.0.0</version>
</dependency>
```

## Usage Examples

### Kotlin

```kotlin
import com.rarnu.ison.ISON

val doc = ISON.parse("""
table.users
id:int name:string active:bool
1 Alice true
2 Bob false
""")

val users = doc.get("users")
for (row in users!!.rows) {
    val name = row["name"]?.asString()
    println(name)
}
```

### Java(17+)

```java
import com.rarnu.ison.ISON;

var doc = ISON.parse("""
table.users
id:int name:string active:bool
1 Alice true
2 Bob false
""");

var users = doc.get("users");
for (var row: users.getRows()) {
    var name = row.get("name").asString();
    System.out.println(name);
}
```
