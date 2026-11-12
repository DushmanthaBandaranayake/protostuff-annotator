# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

IntelliJ IDEA plugin that generates and checks Protostuff `@Tag` annotations in Java and Kotlin classes. Published on the JetBrains Marketplace as plugin id `ProtostuffAnnotator` (Marketplace id 11364). Licensed Apache 2.0.

## Commands

Requires JDK 21. Built with the IntelliJ Platform Gradle Plugin 2.x against IntelliJ IDEA Community 2024.2.6 (`gradle.properties`: `platformVersion`, `pluginSinceBuild = 222`, no until-build). The code is compiled to Java 17 bytecode (`options.release = 17`) because 2022.2-2024.1 run on Java 17, so don't use Java 18+ language features or APIs. 2022.1 (Java 11, no `ActionUpdateThread`) is deliberately unsupported.

```bash
./gradlew test                                                        # all tests
./gradlew test --tests 'com.dush.psannotator.KotlinTagTest'           # one class
./gradlew test --tests 'com.dush.psannotator.KotlinTagTest.testGenerateAddsImportAndTags'  # one method
./gradlew buildPlugin            # build/distributions/protostuff-annotator-<version>.zip (the file to upload)
./gradlew verifyPluginStructure  # descriptor/archive checks
./gradlew verifyPlugin           # Plugin Verifier vs IDEA 2022.2.5, 2024.2.6, 2026.3 (downloads IDEs, ~5 min)
./gradlew runIde                 # sandbox IDE with the plugin installed
```

There is no linter and no CI. Run `verifyPlugin` before a release. The Marketplace re-verifies every listed version against each new IDE, so API removals show up there too.

## Architecture

All code is under `src/main/java/com/dush/psannotator/`. Features are written once against a language-neutral model and work for both Java and Kotlin.

- **`model/`** is the core.
  - `TagClass` and `TagField` describe a class and its fields.
  - `TagModelProvider` is an extension point (`com.dush.psannotator.tagModelProvider`) that builds those models from PSI. `TagModels` is the entry point that asks every provider.
  - `TagPlanner` works out tag numbers without editing anything. `TagOperations` applies the edits and must run inside a write command.
  - `TagHierarchy` is a class plus its editable superclasses and its project subclasses (via `ClassInheritorsSearch` on `TagClass.getPsiClass()`, which is a light class for Kotlin). Its `Scope` drives the Generate and Remove dialogs' "Classes" choice. Build it inside a read action with indexes ready; the actions do it under a progress dialog (`TagTargets.hierarchy`). `TagOperations.generateInScope`/`removeInScope` hold the multi-class loops so tests and actions share them.
- **`java/` and `kotlin/`** hold the two providers.
  - The Kotlin provider and Kotlin-only registrations live in `META-INF/psannotator-kotlin.xml`, an optional `<depends>` on `org.jetbrains.kotlin`. Classes in `kotlin/` must never be referenced from the rest of the code, or the plugin breaks in IDEs without Kotlin.
  - The Kotlin provider is registered `order="first"` because Kotlin light classes are also `PsiClass`es. The Java provider also rejects non-Java languages.
- **Features**, all of which go through the model:
  - `inspection/`: `TagProblems` is the pure check logic; `ProtostuffTagInspection` and `TagFixes` are the IntelliJ inspection and its quick fixes.
  - `intention/AddTagIntention`: Alt+Enter on a single field.
  - `actions/`: Generate, Renumber and Remove. Each works on the class at the caret or, via `TagTargets`, on a Project view selection. They're registered in the Generate (Alt+Insert) group and in the `ProtostuffMenu` group on the editor, Project view and Code menus. `TagActionBase.isApplicableTo` decides per action whether it shows for the class at the caret.
  - `ui/`: the dialogs. `TagSettings` holds the remembered dialog choices.
- **Per-language registrations:** IntelliJ registers inspections and intentions per language, so Kotlin needs its own trivial subclasses (`ProtostuffTagKotlinInspection`, `AddTagKotlinIntention`). Each one needs its own description folder under `src/main/resources/inspectionDescriptions/` or `intentionDescriptions/`.

### Numbering rules (used everywhere)
- The class hierarchy is walked top-down in declaration order (`TagPlanner.hierarchyTopDown`). Numbers already used anywhere in it are never reused. Each untagged inherited field reserves one number.
- Static and `@Exclude` fields are never tagged. Transient fields only when the user asks.
- `planGenerate` fills gaps from a start number. `nextAfterMax` (used by the intention and the quick fixes) never fills gaps, because a gap is usually a removed field.
- Bulk operations process superclasses first (`TagOperations.sortSuperclassesFirst`).

## Things that have bitten us
- **Model identity:** `TagField` and `TagClass` objects are recreated on every `getFields()` call. Their `equals`/`hashCode` compare the underlying PSI declaration. Without that, maps keyed by fields (such as the Generate dialog's planned numbers) silently miss.
- **Imports:** write `@Tag(n)` and add `import io.protostuff.Tag` yourself, even when the class doesn't resolve. `shortenClassReferences` alone leaves `@io.protostuff.Tag` when Protostuff isn't on the classpath. Use the qualified name only when `Tag` already means another class in the file.
- **Kotlin `@field:`:** `io.protostuff.Tag` declares no `@Target`, so on a Kotlin constructor `val`/`var` a plain `@Tag` lands on the constructor parameter, where Protostuff doesn't look. Generated Kotlin tags must be `@field:Tag(n)`, and the inspection flags the other form.
- **Kotlin uses PSI only, no type analysis:** this keeps it working in both K1 and K2 mode. The `<supportsKotlinPluginMode>` declaration must stay in the main `plugin.xml`. In the optional descriptor it is ignored, and the plugin gets disabled in K2 mode.
- **Renumbering:** replace only the value expression, never the whole annotation, so names, `@field:` targets and layout are kept.
- **Platform icons:** icons named in `plugin.xml` (`AllIcons.*`) are resolved at runtime and not checked by the verifier. Confirm a name exists in both the oldest (2022.2) and newest supported IDEs before using it.

## Tests

The tests are light fixture tests (`LightJavaCodeInsightFixtureTestCase`) in `src/test/java/com/dush/psannotator/`. Extend `TagTestCase`:
- **Mock-JDK descriptor:** it uses `DefaultLightProjectDescriptor`, because the default descriptor downloads JetBrains annotations from Maven at test time.
- **Helpers:** `addProtostuff()` adds stub `Tag`/`Exclude` annotations, `edit(...)` runs an operation in a write command, and `check(expected)` compares the document and shows a readable diff (`checkResult` doesn't).
- **Kotlin tests have no stdlib:** for Kotlin highlighting, filter `doHighlighting()` to the plugin's own messages instead of using `checkHighlighting()`.

## Releases
- **Version:** `pluginVersion` in `gradle.properties`.
- **Release notes:** update both `CHANGELOG.md` (Keep a Changelog format) and the `<change-notes>` in `src/main/resources/META-INF/plugin.xml`.
- **Upload:** upload the zip from `build/distributions/`.
- **Description GIF:** the plugin description loads `docs/images/demo.gif` from `master`'s raw GitHub URL, so don't move or rename it.
- **Old Marketplace versions:** version 1.0.1 (2018, uses the removed `WriteCommandAction.Simple`) should have its Marketplace compatibility range capped at `261.*`.
