#ifndef __STRPTIME_H__
#define __STRPTIME_H__

/*! Converts a string representation of time to a time tm structure.
 *
 * param[in]  s  Input character string.
 * param[in]  format  The format argument is a character string that consists of field descriptors and text characters, reminiscent of scanf.
 * param[out] tm  The broken-down time structure defined in <time.h>.
 *
 * return A pointer to the first character not processed in this function call.
 */
char *strptime(const char *s, const char *format, struct tm *tm);


#endif /* __STR_BUILDER_H__ */