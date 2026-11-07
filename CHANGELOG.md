# Changelog

All notable changes to the Protostuff Tag Annotator plugin. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and versions follow [Semantic Versioning](https://semver.org/).

## [1.2.0] - 2026-11-07

### Added
- **Inspection "Protostuff @Tag problems"** for Java and Kotlin. It warns about:
  - a field without `@Tag` in a class where other fields have one (Protostuff requires every serialized field to be tagged once any is), including untagged inherited fields;
  - two serialized fields with the same number, including numbers used in a superclass;
  - numbers below 1 or above 536870911 (2²⁹−1);
  - `@Tag` on a static, transient or `@Exclude` field, which Protostuff ignores.
- **Quick fixes** for those problems: *Tag the remaining fields*, *Tag the remaining fields here and in superclasses*, *Use the next free tag number*, *Remove @Tag*.
- **Alt+Enter intention "Add @Tag(n)"** on a single field. It uses one more than the highest number in the hierarchy and never fills gaps, since a gap is often a removed field whose number shouldn't be reused.
- **Field chooser in the Generate dialog.** It lists the untagged fields with the number each will get, updated live as you change the start number or tick and untick fields. Unticked fields don't use up a number.
- **Work on many classes at once.** Right-click files, packages or folders in the Project view → **Protostuff** → Generate, Renumber or Remove. Generate can continue after the highest tag in each class or start from a fixed number, and can be limited to classes that already use `@Tag`.
- **Renumber Protostuff Tags** renumbers the tagged fields in declaration order, skipping numbers used by superclasses. It warns that data serialized with the old numbers can't be read correctly afterwards.
- **Remove Protostuff Tags** removes every `@Tag` and the import once it's no longer used.
- **Code → Protostuff** menu with Generate, Renumber and Remove.
- **Remembered choices**: "Tag transient fields", "Only classes that already have some @Tag fields", the renumber starting number, and the Generate dialog's size.
- **Kotlin support.** Covers constructor `val`/`var` parameters and body properties with a backing field. Skips computed and delegated properties, `const`, and `object`/companion members. `@Transient` is treated as transient. Works in both K1 and K2 Kotlin modes, and only loads when the Kotlin plugin is installed.
- Fields marked `@io.protostuff.Exclude` are skipped.
- Plugin Verifier configuration (`./gradlew verifyPlugin`) for IDEA 2024.2 and 2026.3.

### Changed
- The Generate dialog's starting number defaults to one more than the highest existing tag when the class already has tags, instead of 1.
- Generate is no longer offered for enums, which Protostuff serializes by number rather than by field.
- The Generate action is now *Generate Protostuff Tags…* (with an ellipsis, since it opens a dialog).

### Fixed
- Renumbering changes only the number and keeps the annotation's name, `@field:` use-site target and layout.
- "Use the next free tag number" ignores out-of-range values, so fixing `@Tag(999999999)` no longer jumps to the maximum.

## [1.1.1] - 2026-11-07

### Fixed
- Always writes `@Tag(n)` and adds `import io.protostuff.Tag`, even when Protostuff can't be resolved yet (not on the classpath, or still indexing). 1.1.0 wrote `@io.protostuff.Tag(n)` in that case.
- No import is added when there's nothing to tag.
- Keeps an existing `import io.protostuff.*`.
- Uses the fully qualified name only when `Tag` already means another class in the file (another `Tag` import, or a `Tag` class in the same package or scope).

## [1.1.0] - 2026-11-07

### Fixed
- Adds the `io.protostuff.Tag` import. Before, a bare `@Tag(n)` was written that didn't compile.
- Running Generate again no longer adds a second `@Tag` to fields that already have one, and existing numbers are never reused.
- The starting number is validated in the dialog (1 to 2²⁹−1). Non-numbers used to crash with `NumberFormatException`.
- Inherited fields are numbered from the top-most superclass down in declaration order, instead of relying on the order `getAllFields()` returns.
- Constants from implemented interfaces are no longer picked up.

### Changed
- Static fields are never tagged (Protostuff doesn't serialize them), and the "Tag Static fields" checkbox is removed.
- The action only appears inside Java classes, not interfaces or annotation types.
- The action id changed from `com.dush` to `com.dush.psannotator.GenerateProtostuffTags`, so shortcuts bound to the old id need setting again.
- Requires IntelliJ IDEA 2024.2 or later (since-build 242, no upper bound).
- Built with the IntelliJ Platform Gradle Plugin 2.x and JDK 21, and includes the Gradle wrapper.
- `plugin.xml` declares its dependencies on the platform and Java modules. Typos fixed.

## [1.0.1] - 2018-11-23

### Added
- First release. **Code → Generate → Generate Protostuff Tags** adds `@Tag` to every field of the class at the caret, from a chosen starting number, with options to include static and transient fields.

[1.2.0]: https://github.com/DushmanthaBandaranayake/protostuff-annotator/compare/9b8ebe5...claude/wizardly-volta-vry5pt
[1.1.1]: https://github.com/DushmanthaBandaranayake/protostuff-annotator/compare/e3220b3...9b8ebe5
[1.1.0]: https://github.com/DushmanthaBandaranayake/protostuff-annotator/compare/7da346e...e3220b3
[1.0.1]: https://github.com/DushmanthaBandaranayake/protostuff-annotator/tree/7da346e
