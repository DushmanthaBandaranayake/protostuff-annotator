# Protostuff `@Tag` annotator
IntelliJ IDEA plugin that adds [Protostuff](https://protostuff.github.io/docs/protostuff-runtime/) `@Tag` annotations to the fields of a Java class.

## How to use
Put the caret inside a class, then **Code → Generate** (Alt+Insert, Cmd+N on macOS) → **Generate Protostuff Tags**.
Enter the starting tag number and choose whether transient fields should be tagged.

- Adds `import io.protostuff.Tag;` when Protostuff is on the classpath (otherwise writes `@io.protostuff.Tag`).
- Fields that already have a `@Tag` are left alone, and their numbers are never reused, so running it again after adding fields is safe.
- Numbers used by superclass fields are reserved (untagged inherited fields reserve one number each, top-most class first), so a subclass doesn't clash with its parents.
- Static fields are never tagged, since Protostuff doesn't serialize them. Transient fields are skipped unless you tick the checkbox.

![Screenshot](https://github.com/DushmanthaBandaranayake/protostuff-annotator/blob/master/docs/images/screen_shot_2.png)
--
![Screenshot](https://github.com/DushmanthaBandaranayake/protostuff-annotator/blob/master/docs/images/screen_shot_3.png)
--
![Screenshot](https://github.com/DushmanthaBandaranayake/protostuff-annotator/blob/master/docs/images/screen_shot_4.png)

## Building
Requires JDK 21. Supports IntelliJ IDEA 2024.2 and later.

```bash
./gradlew buildPlugin   # plugin zip in build/distributions/
./gradlew test          # run the tests
./gradlew runIde        # start a sandbox IDE with the plugin installed
```
