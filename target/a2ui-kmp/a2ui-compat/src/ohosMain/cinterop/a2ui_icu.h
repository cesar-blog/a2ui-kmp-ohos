#ifndef A2UI_OHOS_ICU_H
#define A2UI_OHOS_ICU_H

/* OpenHarmony public Native SDK ICU APIs. All allocated strings are owned by the caller. */
#include <stdlib.h>
#include <string.h>
#include <unicode/uloc.h>
#include <unicode/ustring.h>
#include <unicode/udat.h>
#include <unicode/unum.h>
#include <unicode/ucal.h>

static inline void a2ui_icu_free(void *value) { free(value); }

static inline UChar *a2ui_icu_utf16(const char *text, int32_t *length, UErrorCode *status) {
    int32_t needed = 0;
    u_strFromUTF8(NULL, 0, &needed, text, -1, status);
    if (*status != U_BUFFER_OVERFLOW_ERROR && U_FAILURE(*status)) return NULL;
    *status = U_ZERO_ERROR;
    UChar *out = (UChar *)malloc((needed + 1) * sizeof(UChar));
    if (!out) { *status = U_MEMORY_ALLOCATION_ERROR; return NULL; }
    u_strFromUTF8(out, needed + 1, length, text, -1, status);
    if (U_FAILURE(*status)) { free(out); return NULL; }
    return out;
}

static inline char *a2ui_icu_utf8(const UChar *text, int32_t length, UErrorCode *status) {
    int32_t needed = 0;
    u_strToUTF8(NULL, 0, &needed, text, length, status);
    if (*status != U_BUFFER_OVERFLOW_ERROR && U_FAILURE(*status)) return NULL;
    *status = U_ZERO_ERROR;
    char *out = (char *)malloc(needed + 1);
    if (!out) { *status = U_MEMORY_ALLOCATION_ERROR; return NULL; }
    u_strToUTF8(out, needed + 1, NULL, text, length, status);
    if (U_FAILURE(*status)) { free(out); return NULL; }
    return out;
}

static inline const char *a2ui_icu_default_locale(void) { return uloc_getDefault(); }

/* kind: 0 = language, 1 = country, 2 = BCP 47 tag. */
static inline char *a2ui_icu_locale_part(const char *locale, int kind, int *error) {
    UErrorCode status = U_ZERO_ERROR;
    char buffer[1024];
    int32_t length;
    if (kind == 0) length = uloc_getLanguage(locale, buffer, sizeof(buffer), &status);
    else if (kind == 1) length = uloc_getCountry(locale, buffer, sizeof(buffer), &status);
    else length = uloc_toLanguageTag(locale, buffer, sizeof(buffer), 0, &status);
    *error = (int)status;
    if (U_FAILURE(status)) return NULL;
    /* ICU can return U_STRING_NOT_TERMINATED_WARNING when length equals capacity. */
    if (length < 0 || length >= (int32_t)sizeof(buffer)) {
        *error = (int)U_BUFFER_OVERFLOW_ERROR;
        return NULL;
    }
    char *result = (char *)malloc((size_t)length + 1);
    if (!result) { *error = (int)U_MEMORY_ALLOCATION_ERROR; return NULL; }
    memcpy(result, buffer, (size_t)length);
    result[length] = '\0';
    return result;
}

static inline char *a2ui_icu_locale_from_tag(const char *tag, int *parsed, int *error) {
    UErrorCode status = U_ZERO_ERROR;
    char buffer[1024];
    int32_t length = uloc_forLanguageTag(tag, buffer, sizeof(buffer), parsed, &status);
    *error = (int)status;
    if (U_FAILURE(status)) return NULL;
    if (length < 0 || length >= (int32_t)sizeof(buffer)) {
        *error = (int)U_BUFFER_OVERFLOW_ERROR;
        return NULL;
    }
    char *result = (char *)malloc((size_t)length + 1);
    if (!result) { *error = (int)U_MEMORY_ALLOCATION_ERROR; return NULL; }
    memcpy(result, buffer, (size_t)length);
    result[length] = '\0';
    return result;
}

static inline char *a2ui_icu_default_zone(int *error) {
    UErrorCode status = U_ZERO_ERROR;
    UChar zone[256];
    int32_t length = ucal_getDefaultTimeZone(zone, 256, &status);
    char *out = U_FAILURE(status) ? NULL : a2ui_icu_utf8(zone, length, &status);
    *error = (int)status;
    return out;
}

static inline UDateFormat *a2ui_icu_date_open(const char *pattern, const char *locale,
                                             const char *zone, int lenient, UErrorCode *status) {
    int32_t patternLength = 0, zoneLength = 0;
    UChar *p = a2ui_icu_utf16(pattern, &patternLength, status);
    if (U_FAILURE(*status)) return NULL;
    UChar *z = a2ui_icu_utf16(zone, &zoneLength, status);
    if (U_FAILURE(*status)) { free(p); return NULL; }
    char gregorian[1024];
    if (strlen(locale) >= sizeof(gregorian)) {
        free(p); free(z); *status = U_ILLEGAL_ARGUMENT_ERROR; return NULL;
    }
    strcpy(gregorian, locale);
    uloc_setKeywordValue("calendar", "gregorian", gregorian, sizeof(gregorian), status);
    UDateFormat *format = U_FAILURE(*status) ? NULL :
        udat_open(UDAT_PATTERN, UDAT_PATTERN, gregorian, z, zoneLength, p, patternLength, status);
    free(p); free(z);
    if (format) udat_setLenient(format, lenient != 0);
    return format;
}

static inline char *a2ui_icu_date_format(const char *pattern, const char *locale, const char *zone,
                                        int lenient, double millis, int *error) {
    UErrorCode status = U_ZERO_ERROR;
    UDateFormat *format = a2ui_icu_date_open(pattern, locale, zone, lenient, &status);
    if (!format || U_FAILURE(status)) { *error = (int)status; return NULL; }
    int32_t needed = udat_format(format, millis, NULL, 0, NULL, &status);
    if (status == U_BUFFER_OVERFLOW_ERROR) status = U_ZERO_ERROR;
    UChar *buffer = U_FAILURE(status) ? NULL : (UChar *)malloc((needed + 1) * sizeof(UChar));
    if (!buffer && !U_FAILURE(status)) status = U_MEMORY_ALLOCATION_ERROR;
    char *out = NULL;
    if (!U_FAILURE(status)) {
        int32_t length = udat_format(format, millis, buffer, needed + 1, NULL, &status);
        if (!U_FAILURE(status)) out = a2ui_icu_utf8(buffer, length, &status);
    }
    free(buffer); udat_close(format); *error = (int)status;
    return out;
}

static inline double a2ui_icu_date_parse(const char *pattern, const char *locale, const char *zone,
                                        int lenient, const char *text, int *position, int *error) {
    UErrorCode status = U_ZERO_ERROR;
    UDateFormat *format = a2ui_icu_date_open(pattern, locale, zone, lenient, &status);
    if (!format || U_FAILURE(status)) { *error = (int)status; return 0; }
    int32_t length = 0;
    UChar *input = a2ui_icu_utf16(text, &length, &status);
    int32_t consumed = 0;
    double result = 0;
    if (!U_FAILURE(status)) result = udat_parse(format, input, length, &consumed, &status);
    *position = consumed;
    free(input); udat_close(format); *error = (int)status;
    return result;
}

static inline char *a2ui_icu_date_pattern(const char *locale, int style, int time, int *error) {
    UErrorCode status = U_ZERO_ERROR;
    UDateFormat *format = udat_open(time ? (UDateFormatStyle)style : UDAT_NONE,
        time ? UDAT_NONE : (UDateFormatStyle)style, locale, NULL, 0, NULL, 0, &status);
    if (!format || U_FAILURE(status)) { *error = (int)status; return NULL; }
    int32_t needed = udat_toPattern(format, 0, NULL, 0, &status);
    if (status == U_BUFFER_OVERFLOW_ERROR) status = U_ZERO_ERROR;
    UChar *buffer = U_FAILURE(status) ? NULL : (UChar *)malloc((needed + 1) * sizeof(UChar));
    if (!buffer && !U_FAILURE(status)) status = U_MEMORY_ALLOCATION_ERROR;
    char *out = NULL;
    if (!U_FAILURE(status)) {
        int32_t length = udat_toPattern(format, 0, buffer, needed + 1, &status);
        if (!U_FAILURE(status)) out = a2ui_icu_utf8(buffer, length, &status);
    }
    free(buffer); udat_close(format); *error = (int)status;
    return out;
}

/* style: 0 = decimal, 1 = currency, 2 = percent. */
static inline UNumberFormat *a2ui_icu_number_open(const char *locale, int style, UErrorCode *status) {
    UNumberFormatStyle nativeStyle = style == 1 ? UNUM_CURRENCY : style == 2 ? UNUM_PERCENT : UNUM_DECIMAL;
    return unum_open(nativeStyle, NULL, 0, locale, NULL, status);
}

static inline int a2ui_icu_number_default(const char *locale, int style, int attribute, int *error) {
    UErrorCode status = U_ZERO_ERROR;
    UNumberFormat *format = a2ui_icu_number_open(locale, style, &status);
    if (!format || U_FAILURE(status)) { *error = (int)status; return 0; }
    UNumberFormatAttribute attr = attribute == 0 ? UNUM_MIN_FRACTION_DIGITS :
        attribute == 1 ? UNUM_MAX_FRACTION_DIGITS : UNUM_GROUPING_USED;
    int result = unum_getAttribute(format, attr);
    unum_close(format); *error = (int)status; return result;
}

static inline char *a2ui_icu_number_currency(const char *locale, int style, int *error) {
    UErrorCode status = U_ZERO_ERROR;
    UNumberFormat *format = a2ui_icu_number_open(locale, style, &status);
    if (!format || U_FAILURE(status)) { *error = (int)status; return NULL; }
    UChar code[16];
    int32_t length = unum_getTextAttribute(format, UNUM_CURRENCY_CODE, code, 16, &status);
    char *out = U_FAILURE(status) ? NULL : a2ui_icu_utf8(code, length, &status);
    unum_close(format); *error = (int)status; return out;
}

static inline char *a2ui_icu_number_format(const char *locale, int style, int grouping,
                                         int minimum, int maximum, const char *currency,
                                         const char *decimal, double value, int *error) {
    UErrorCode status = U_ZERO_ERROR;
    UNumberFormat *format = a2ui_icu_number_open(locale, style, &status);
    if (!format || U_FAILURE(status)) { *error = (int)status; return NULL; }
    if (currency) {
        int32_t length = 0;
        UChar *code = a2ui_icu_utf16(currency, &length, &status);
        if (!U_FAILURE(status)) unum_setTextAttribute(format, UNUM_CURRENCY_CODE, code, length, &status);
        free(code);
    }
    unum_setAttribute(format, UNUM_GROUPING_USED, grouping);
    unum_setAttribute(format, UNUM_MIN_FRACTION_DIGITS, minimum);
    unum_setAttribute(format, UNUM_MAX_FRACTION_DIGITS, maximum);
    int32_t needed = 0;
    if (!U_FAILURE(status)) {
        needed = decimal ? unum_formatDecimal(format, decimal, -1, NULL, 0, NULL, &status) :
            unum_formatDouble(format, value, NULL, 0, NULL, &status);
    }
    if (status == U_BUFFER_OVERFLOW_ERROR) status = U_ZERO_ERROR;
    UChar *buffer = U_FAILURE(status) ? NULL : (UChar *)malloc((needed + 1) * sizeof(UChar));
    if (!buffer && !U_FAILURE(status)) status = U_MEMORY_ALLOCATION_ERROR;
    char *out = NULL;
    if (!U_FAILURE(status)) {
        int32_t length = decimal ? unum_formatDecimal(format, decimal, -1, buffer, needed + 1, NULL, &status) :
            unum_formatDouble(format, value, buffer, needed + 1, NULL, &status);
        if (!U_FAILURE(status)) out = a2ui_icu_utf8(buffer, length, &status);
    }
    free(buffer); unum_close(format); *error = (int)status; return out;
}

/* operation: 0 = get field, 1 = set field and return epoch millis, 2 = clear. */
static inline double a2ui_icu_calendar(const char *zone, double millis, int field, int value,
                                      int operation, int *error) {
    UErrorCode status = U_ZERO_ERROR;
    int32_t length = 0;
    UChar *zoneId = a2ui_icu_utf16(zone, &length, &status);
    UCalendar *calendar = U_FAILURE(status) ? NULL : ucal_open(zoneId, length, "en_US", UCAL_GREGORIAN, &status);
    free(zoneId);
    if (!calendar || U_FAILURE(status)) { *error = (int)status; return 0; }
    ucal_setMillis(calendar, millis, &status);
    double result = 0;
    if (!U_FAILURE(status)) {
        if (operation == 0) result = ucal_get(calendar, (UCalendarDateFields)field, &status);
        else {
            if (operation == 1) ucal_set(calendar, (UCalendarDateFields)field, value);
            else ucal_clear(calendar);
            result = ucal_getMillis(calendar, &status);
        }
    }
    ucal_close(calendar); *error = (int)status; return result;
}

#endif
