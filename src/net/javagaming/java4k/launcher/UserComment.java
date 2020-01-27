/*
 * This is free and unencumbered software released into the public domain.
 *
 * Anyone is free to copy, modify, publish, use, compile, sell, or
 * distribute this software, either in source code form or as a compiled
 * binary, for any purpose, commercial or non-commercial, and by any
 * means.
 *
 * In jurisdictions that recognize copyright laws, the author or authors
 * of this software dedicate any and all copyright interest in the
 * software to the public domain. We make this dedication for the benefit
 * of the public at large and to the detriment of our heirs and
 * successors. We intend this dedication to be an overt act of
 * relinquishment in perpetuity of all present and future rights to this
 * software under copyright law.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
 * For more information, please refer to &lt;http://unlicense.org/
 */
package net.javagaming.java4k.launcher;

import java.util.Date;

/**
 *
 * @author Groboclown
 */
public class UserComment implements Comparable<UserComment> {
    private final String id;
    private final String username;
    private final Date postDate;
    private final String text;

    public UserComment(String id, String username, Date postDate, String text) {
        this.id = id;
        this.username = username;
        this.postDate = postDate;
        this.text = text;
    }

    public String getId() {
        return id;
    }


    public String getUsername() {
        return username;
    }

    public Date getPostDate() {
        return postDate;
    }

    public String getText() {
        return text;
    }

    @Override
    public int compareTo(UserComment o) {
        if (o == null) {
            return 1;
        }
        return postDate.after(o.getPostDate()) ? 1 :
                postDate.before(o.getPostDate()) ? -1 :
                        0;
    }
}
