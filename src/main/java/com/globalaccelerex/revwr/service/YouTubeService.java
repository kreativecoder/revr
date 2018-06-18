/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.globalaccelerex.revwr.service;

import com.globalaccelerex.revwr.controller.RevwrController;
import org.springframework.stereotype.Service;
import java.util.List;

import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Comment;
import com.google.api.services.youtube.model.CommentThread;
import com.google.api.services.youtube.model.CommentThreadListResponse;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

/**
 *
 * @author Abiola.Adebanjo
 */
@Service
public class YouTubeService {

    Logger logger = LoggerFactory.getLogger(YouTubeService.class);
    
    private static YouTube youtube;
    public static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
    public static final JsonFactory JSON_FACTORY = new JacksonFactory();

    List<String> scopes = Lists.newArrayList("https://www.googleapis.com/auth/youtube.force-ssl");

    int counter;

    List<Comment> comments = Lists.newArrayList();
    
    @Value("${youtube.api.key}")
    String apiKey;

    public List<Comment> getComments(String videoId) throws Exception {
        logger.info("Key is >> " + apiKey);
        youtube = new YouTube.Builder(new NetHttpTransport(), new JacksonFactory(),
                (request) -> {
                }).setApplicationName("revr").build();

        CommentThreadListResponse commentsPage = prepareListRequest(videoId).execute();

        while (true) {
            handleCommentsThreads(commentsPage.getItems());

            String nextPageToken = commentsPage.getNextPageToken();
            if (nextPageToken == null) {
                break;
            }

            // Get next page of video comments threads
            commentsPage = prepareListRequest(videoId).setPageToken(nextPageToken).execute();
        }

        return comments;
    }

    private YouTube.Comments.List prepareCommentReply(String parentId) throws Exception {
        return youtube.comments()
                .list("snippet")
                .setKey(apiKey)
                .setParentId(parentId)
                .setMaxResults(100L)
                .setTextFormat("plainText");
    }

    private YouTube.CommentThreads.List prepareListRequest(String videoId) throws Exception {
        return youtube.commentThreads()
                .list("snippet,replies")
                .setKey(apiKey)
                .setVideoId(videoId)
                .setMaxResults(100L)
                .setTextFormat("plainText");
    }

    private void handleCommentsThreads(List<CommentThread> commentThreads) throws Exception {
        for (CommentThread commentThread : commentThreads) {
            comments.add(commentThread.getSnippet().getTopLevelComment());
            if (commentThread.getReplies() != null) {
                comments.addAll(prepareCommentReply(commentThread.getSnippet().getTopLevelComment().getId()).execute().getItems());
            }
        }
    }
}
