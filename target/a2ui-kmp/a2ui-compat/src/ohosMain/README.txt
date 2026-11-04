OHOS compatibility implementation

This source set implements the existing common expect declarations with public Native SDK ICU
and POSIX APIs. It does not contain an Android/JVM runtime or reuse iOS Foundation APIs.

Build requirements
- a2uiIcu cinterop: cinterop/a2ui_icu.def, package ai.littlepress.a2ui.compat.icu, link -licu.
- Native SDK public headers unicode/uloc.h, udat.h, unum.h, ucal.h and ustring.h.
- pthread and gettimeofday from platform.posix.

Host services
Register OhosA2uiPlatform.configure(openUrl, localeTag, is24Hour) from application startup.
The opener must perform actual Ability/ArkTS URL launching and dispatch to the correct thread.
If no opener is registered, calling openUrl throws; it never reports a discarded request as success.
Without preference providers, locale uses ICU's process default and clock format is inferred from
that locale. These fallbacks do not claim to read the user's HarmonyOS preferences.

Scope and differences that require testing
- Date/number/currency/percent formatting uses the device's ICU data. The upstream Android digit
  defaults are retained, but a different ICU/CLDR version can change localized strings and spacing.
- The existing AndroidDatePattern rewriting is reused for formatting. Parsing a Java 'u' weekday
  pattern is explicitly unsupported; it throws rather than parsing the wrong field as a year.
- Locale.forLanguageTag accepts complete BCP 47 tags and rejects malformed/truncated tags. This is
  stricter than java.util.Locale.forLanguageTag, which can silently keep the valid prefix.
- Locale conversion output is limited to 1023 bytes; longer output reports an ICU overflow error.
- Embedded NUL characters in strings sent to the C bridge are explicitly rejected.
- Currency validation uses a recorded java.util.Currency.getAvailableCurrencies() snapshot from
  OpenJDK on 2026-11-04, including historical ISO 4217 codes. It does not accept arbitrary triples.
- Locks use POSIX mutexes and condition variables with recursive ownership and fair ticket ordering.
  ConcurrentHashMap serializes operations; CopyOnWriteArrayList iterators capture a stable snapshot.

Validation
- a2ui_icu.h compiles against the public DevEco aarch64 Native SDK headers.
- icu_smoke_test.c exercises locale, epoch dates, leap-day rejection, decimal/currency/percent,
  Gregorian field normalization, local-zone clear, and repeated formatter allocation/release.
- Host ICU execution checks the bridge logic only. Run the same test on OHOS before claiming
  runtime compatibility with that device's ICU or Android parity across the complete upstream suite.
