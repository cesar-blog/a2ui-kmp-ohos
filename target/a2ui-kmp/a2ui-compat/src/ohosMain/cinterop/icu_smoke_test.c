/* Standalone Native SDK smoke test. Run on OHOS to verify the device's ICU, not just host ICU. */
#include <assert.h>
#include <stdio.h>
#include "a2ui_icu.h"

/* BCP 47 permits arbitrarily long private-use sequences of 1..8 character subtags. */
static void private_use_tag(char *tag, int length) {
    memcpy(tag, "en-x-", 5);
    int offset = 5;
    while (offset < length) {
        int remaining = length - offset;
        int count = remaining > 8 ? 8 : remaining;
        if (remaining - count == 1) count--;
        memset(tag + offset, 'a', (size_t)count);
        offset += count;
        if (offset < length) tag[offset++] = '-';
    }
    tag[length] = '\0';
}

static void locale_buffer_boundaries(void) {
    for (int length = 1023; length <= 1025; length++) {
        char tag[1026], localeId[1026];
        private_use_tag(tag, length);
        /* ICU's corresponding private-use locale ID has the same length. */
        memcpy(localeId, "en@x=", 5);
        memcpy(localeId + 5, tag + 5, (size_t)length - 5 + 1);
        int error = 0, parsed = 0;
        char *result = a2ui_icu_locale_from_tag(tag, &parsed, &error);
        if (length == 1023) {
            assert(error <= 0 && result && parsed == length && strlen(result) == (size_t)length);
        } else {
            assert(result == NULL && error == U_BUFFER_OVERFLOW_ERROR);
        }
        free(result);
        result = a2ui_icu_locale_part(localeId, 2, &error);
        if (length == 1023) {
            assert(error <= 0 && result && strcmp(result, tag) == 0);
        } else {
            assert(result == NULL && error == U_BUFFER_OVERFLOW_ERROR);
        }
        free(result);
    }
}

int main(void) {
    locale_buffer_boundaries();
    int error = 0, parsed = 0;
    char *locale = a2ui_icu_locale_from_tag("zh-Hans-CN", &parsed, &error);
    assert(error <= 0 && parsed == 10 && strcmp(locale, "zh_Hans_CN") == 0);
    char *language = a2ui_icu_locale_part(locale, 0, &error);
    assert(error <= 0 && strcmp(language, "zh") == 0);
    free(language);
    free(locale);

    char *text = a2ui_icu_date_format("yyyy-MM-dd'T'HH:mm:ssZ", "en_US", "UTC", 0, 0, &error);
    assert(error <= 0 && strcmp(text, "1970-01-01T00:00:00+0000") == 0);
    free(text);
    double millis = a2ui_icu_date_parse("yyyy-MM-dd", "en_US", "UTC", 0, "2024-02-29", &parsed, &error);
    assert(error <= 0 && parsed == 10 && millis == 1709164800000.0);
    a2ui_icu_date_parse("yyyy-MM-dd", "en_US", "UTC", 0, "2023-02-29", &parsed, &error);
    assert(error > 0);

    error = 0;
    text = a2ui_icu_number_format("en_US", 0, 1, 0, 3, NULL, "1234.5", 1234.5, &error);
    assert(error <= 0 && strcmp(text, "1,234.5") == 0);
    free(text);
    text = a2ui_icu_number_format("en_US", 1, 1, 2, 2, "JPY", "1234.56", 1234.56, &error);
    assert(error <= 0 && strcmp(text, "¥1,234.56") == 0);
    free(text);
    text = a2ui_icu_number_format("en_US", 2, 1, 0, 0, NULL, "0.125", 0.125, &error);
    assert(error <= 0 && strcmp(text, "12%") == 0);
    free(text);

    assert(a2ui_icu_calendar("UTC", 0, 1, 0, 0, &error) == 1970 && error <= 0);
    assert(a2ui_icu_calendar("UTC", 0, 11, 25, 1, &error) == 90000000 && error <= 0);
    assert(a2ui_icu_calendar("Asia/Shanghai", 0, 0, 0, 2, &error) == -28800000 && error <= 0);
    for (int i = 0; i < 1000; i++) {
        text = a2ui_icu_date_format("yyyy-MM-dd", "zh_CN", "UTC", 0, i, &error);
        assert(text && error <= 0);
        free(text);
    }
    puts("A2UI ICU smoke assertions passed");
    return 0;
}
