<p align="center">
  <a href="https://suicookbook.com/">
    <img src="images/mbl.png" alt="MBL logo" width="200" height="165">
  </a>
</p>

<h1 align="center">MBL</h1>

MBL is a versatile framework for low-level manipulation and analysis of Move bytecode.
It enables direct editing of existing modules and supports dynamic generation of new modules in raw binary format, 
without requiring source-level transformations.

> [!WARNING]  
> MBL is currently experimental and the API might be subject to rapid changes


Here is a sample code of how to use it:

```kotlin
 val module = Mbl.deserializeModule(originalBytes).getOrThrow {
     throw Exception("Deserialization Failure")
}

val editedModule = module.edit {
    replaceConstantString("Template Coin", "My Awesome Coin")
    replaceConstantString("TMPL", "AWSM")
    replaceConstantString("Template Coin Description", "An awesome new coin.")
}

val editedBytes = Mbl.serializeModule(editedModule)
```

## License

    Copyright 2025 McXross

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.