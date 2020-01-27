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

import net.javagaming.java4k.launcher.cache.Resource;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Standard applet with a URL source.
 *
 * @author Groboclown
 */
public class DefaultGameDescription implements GameDescription {
    private String name;
    private String id;
    private String author;
    private String authorContact;
    private String description;
    private String instructions;
    private Date submissionDate;
    private final Map<String, String> serverInfo =
            new HashMap<String, String>();
    private Resource iconResource;
    private final List<UserComment> userComments =
            new ArrayList<UserComment>();
    //private GameDetail gameDetail;
    private Resource detailSource;
    private SourceType type;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getAuthor() {
        return author;
    }

    @Override
    public String getAuthorContact() {
        return authorContact;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getInstructions() {
        return instructions;
    }

    @Override
    public Resource getGameDetailSource() {
        return detailSource;
    }

    @Override
    public Date getSubmissionDate() {
        return submissionDate;
    }

    @Override
    public Map<String, String> getServerInfo() {
        return serverInfo;
    }

    @Override
    public Resource getIconResource() {
        return iconResource;
    }

    @Override
    public List<UserComment> getUserComments() {
        return userComments;
    }

    @Override
    public String toString() {
        return getName();
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public void setAuthorContact(String authorContact) {
        this.authorContact = authorContact;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public void setSubmissionDate(Date submissionDate) {
        this.submissionDate = submissionDate;
    }

    public void setServerInfo(Map<String, String> serverInfo) {
        this.serverInfo.clear();
        if (serverInfo != null) {
            this.serverInfo.putAll(serverInfo);
        }
    }

    public void addServerInfo(String key, String value) {
        serverInfo.put(key, value);
    }

    public void setIconResource(Resource iconResource) {
        this.iconResource = iconResource;
    }

    public void setUserComments(List<UserComment> userComments) {
        this.userComments.clear();
        if (userComments != null) {
            this.userComments.addAll(userComments);
        }
    }

    public void addUserComments(List<UserComment> userComments) {
        if (userComments != null) {
            this.userComments.addAll(userComments);
        }
    }

    public void setDetailSource(Resource detailSource) {
        this.detailSource = detailSource;
    }

    @Override
    public SourceType getSourceType() {
        return type;
    }

    public void setSourceType(SourceType type) {
        this.type = type;
    }

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
