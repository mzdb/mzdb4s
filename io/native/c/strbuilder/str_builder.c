/*
Sources:
- https://codereview.stackexchange.com/questions/155286/stringbuilder-in-c
- http://coliru.stacked-crooked.com/a/5346f2879261d421
- https://nachtimwald.com/2017/02/26/efficient-c-string-builder/
- https://github.com/cavaliercoder/c-stringbuilder/blob/master/sb.c
*/

#include <ctype.h>
#include <stdbool.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>

#include "str_builder.h"

static const size_t str_builder_min_size = 32;

/* - - - - */
 
str_builder_t *str_builder_create(void)
{
    str_builder_t *sb;
 
    sb          = calloc(1, sizeof(*sb));
    sb->str     = malloc(str_builder_min_size);
    *sb->str    = '\0';
    sb->alloced = str_builder_min_size;
    sb->len     = 0;
 
    return sb;
}
 
void str_builder_destroy(str_builder_t *sb)
{
    if (sb == NULL)
        return;
    free(sb->str);
    free(sb);
}
 
/* - - - - */
 
/*! Ensure there is enough space for data being added plus a NULL terminator.
 *
 * \param[in,out] sb      Builder.
 * \param[in]     add_len The length that needs to be added *not* including a NULL terminator.
 */
static void str_builder_ensure_space(str_builder_t *sb, size_t add_len)
{
    if (sb == NULL || add_len == 0)
        return;
 
    if (sb->alloced >= sb->len+add_len+1)
        return;
 
    while (sb->alloced < sb->len+add_len+1) {
        /* Doubling growth strategy. */
        sb->alloced <<= 1;
        if (sb->alloced == 0) {
            /* Left shift of max bits will go to 0. An unsigned type set to
             * -1 will return the maximum possible size. However, we should
             *  have run out of memory well before we need to do this. Since
             *  this is the theoretical maximum total system memory we don't
             *  have a flag saying we can't grow any more because it should
             *  be impossible to get to this point. */
            sb->alloced--;
        }
    }
    sb->str = realloc(sb->str, sb->alloced);
}
 
/* - - - - */
 
void str_builder_add_str(str_builder_t *sb, const char *str, size_t len)
{
    if (sb == NULL || str == NULL || *str == '\0')
        return;
 
    if (len == 0)
        len = strlen(str);
 
    str_builder_ensure_space(sb, len);
    memmove(sb->str+sb->len, str, len);
    sb->len += len;
    sb->str[sb->len] = '\0';
}
 
void str_builder_add_char(str_builder_t *sb, char c)
{
    if (sb == NULL)
        return;
    str_builder_ensure_space(sb, 1);
    sb->str[sb->len] = c;
    sb->len++;
    sb->str[sb->len] = '\0';
}
 
void str_builder_add_int(str_builder_t *sb, int val)
{
    char str[12];
 
    if (sb == NULL)
        return;
     
    snprintf(str, sizeof(str), "%d", val);
    str_builder_add_str(sb, str, 0);
}

/* - - - - */

void str_builder_clear(str_builder_t *sb)
{
    if (sb == NULL)
        return;
    str_builder_truncate(sb, 0);
}

void str_builder_truncate(str_builder_t *sb, size_t len)
{
    if (sb == NULL || len >= sb->len)
        return;

    sb->len = len;
    sb->str[sb->len] = '\0';
}

void str_builder_drop(str_builder_t *sb, size_t len)
{
    if (sb == NULL || len == 0)
        return;

    if (len >= sb->len) {
        str_builder_clear(sb);
        return;
    }

    sb->len -= len;
    /* +1 to move the NULL. */
    memmove(sb->str, sb->str+len, sb->len+1);
}

/* - - - - */

size_t str_builder_len(const str_builder_t *sb)
{
    if (sb == NULL)
        return 0;
    return sb->len;
}
 
const char *str_builder_peek(const str_builder_t *sb)
{
    if (sb == NULL)
        return NULL;
    return sb->str;
}

char *str_builder_dump(const str_builder_t *sb, size_t *len)
{
    char *out;

    if (sb == NULL)
        return NULL;

    if (len != NULL)
        *len = sb->len;
    out = malloc(sb->len+1);
    memcpy(out, sb->str, sb->len+1);
    return out;
}

// TODO: move to dedicated misc_utils.c library

// See:
// - https://www.geeksforgeeks.org/convert-floating-point-number-string/
// - https://www.geeksforgeeks.org/implement-itoa/
// - http://www.strudel.org.uk/itoa/
// - https://en.wikibooks.org/wiki/C_Programming/stdlib.h/itoa
// - https://github.com/miloyip/itoa-benchmark/
// - https://stackoverflow.com/questions/190229/where-is-the-itoa-function-in-linux
// - https://stackoverflow.com/questions/2637714/is-it-possible-to-roll-a-significantly-faster-version-of-modf
// - https://stackoverflow.com/questions/19339043/sse-version-of-modf
#include <math.h>
//#include <stdio.h>

// Reverses a string 'str' of length 'len'
void reverse(char* str, int len)
{
    int i = 0, j = len - 1, temp;
    while (i < j) {
        temp = str[i];
        str[i] = str[j];
        str[j] = temp;
        i++;
        j--;
    }
}

// Implementation of itoa()
int itoa(int n, char s[])
{
    int i, sign;

    if ((sign = n) < 0)  /* record sign */
        n = -n;          /* make n positive */

    i = 0;
    do {
        /* generate digits in reverse order */
        s[i++] = n % 10 + '0';   /* get next digit */
    } while ((n /= 10) > 0);     /* delete it */

    if (sign < 0)
        s[i++] = '-';

    reverse(s, i);
    s[i] = '\0';

    return i;
}

// Converts a given fractional part fp to string str[].
// d is the number of digits required in the output.
// If d is more than the number of digits in x, then 0s are added at the beginning.
int fptoa(int fp, char str[], int d)
{
    if (fp == 0) {
        str[0] = '0';
        str[1] = '\0';
        return 1;
    }

    int i = 0;
    int remainder = 0;
    int remainderSum = 0;

    while (fp) {
        remainder = (fp % 10);
        remainderSum += remainder;

        str[i++] = remainderSum == 0 ? '\0' : remainder + '0';

        fp = fp / 10;
    }

    // If number of digits required is more, then add 0s at the beginning
    while (i < d)
        str[i++] = '0';

    reverse(str, i);

    str[i] = '\0';

    return strlen(str);
}

// Converts a double number to a string.
int dtoa(double v, char* res, int places)
{
    // If no decimal places expected then treat value as integer
    if (places == 0) {
        return itoa((int)round(v), res);
    }

    // Extract integer part
    int ipart = (int)v;

    // Extract floating part
    double fpart = v - (double)ipart;

    // Compute scale corresponding to desired precision
    static const double pow10[] = { 1, 10, 100, 1000, 10000, 100000, 1000000, 10000000, 100000000, 1000000000 };
    int scale = places > 9 ? pow(10, places) : pow10[places];

    // Round the value of fraction part upto given number of decimal places after dot.
    int rfpart = (int)round(fpart * scale);
    if (rfpart == scale)
        ipart += 1;

    // Convert integer part to string
    int ilen = itoa(ipart, res);

    res[ilen] = '.'; // add dot

    // Handle cases like 244.9998474 which should round up to 245.0 if places <= 3
    if (rfpart == scale) {
        res[ilen + 1] = '0';
        res[ilen + 2] = '\0';
        return ilen + 2;
    }

    // The places parameter is needed to handle cases like 233.007
    int fplen = fptoa(rfpart, res + ilen + 1, places);

    return ilen + 1 + fplen;
}

// Converts a floating-point number to a string.
int ftoa(float v, char* res, int places)
{
    // If no decimal places expected then treat value as integer
    if (places == 0) {
        return itoa((int)round(v), res);
    }

    // We fallback to double representation to be closer to JVM outputs
    return dtoa( (double)v, res, places);
}
