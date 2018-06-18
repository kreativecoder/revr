package com.globalaccelerex.revwr.controller;

import com.globalaccelerex.revwr.model.Search;
import com.globalaccelerex.revwr.service.YouTubeService;
import com.google.api.services.youtube.model.Comment;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.List;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import org.apache.commons.lang.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

/**
 *
 * @author Abiola.Adebanjo
 */
@Controller
public class RevwrController {

    @Autowired
    private YouTubeService youtubeService;
    Logger logger = LoggerFactory.getLogger(RevwrController.class);

    @GetMapping(value = "/")
    public String index(Model model) {
        model.addAttribute("search", new Search());
        return "index";
    }

    @PostMapping("/")
    public String formSubmit(@Valid Search search, BindingResult bindingResult, Model model, HttpServletResponse response) {
        if (bindingResult.hasErrors()) {
            logger.error("has errors");
            return "index";
        }

        try {
            URL url = new URL(search.getLink());
            if (url.getHost().equals("www.youtube.com")) {
                //query param is of the form v=BjZbAzUM9Ao, remove the first 2 to get the videoId
                String videoId = url.getQuery().substring(2);
                List<Comment> comments = youtubeService.getComments(videoId);

                response.addHeader("Content-Type", "application/csv");
                response.addHeader("Content-Disposition", "attachment; filename=comments.csv");
                response.setCharacterEncoding("UTF-8");

                writeCSVToOutput(response.getWriter(), comments);
            } else {
                bindingResult.addError(new FieldError("search", "link", "Please provide a youtube link"));
                return "index";
            }
        } catch (MalformedURLException ex) {
            bindingResult.addError(new FieldError("search", "link", ex.getMessage()));
            return "index";
        } catch (Exception ex) {
            //dont do this in prod, dont return direct error message to user
            bindingResult.addError(new FieldError("search", "link", ex.getMessage()));
            return "index";
        }
        
        return "index";
    }

    private void writeCSVToOutput(PrintWriter out, List<Comment> comments) {
        SimpleDateFormat sf = new SimpleDateFormat("MMMM dd, yyyy");
        StringBuilder sb = new StringBuilder();
        sb.append("UserName,Date,Likes,Comment");
        sb.append("\n");
        comments.forEach(comment -> {
            sb.append(StringEscapeUtils.escapeCsv(comment.getSnippet().getAuthorDisplayName()));
            sb.append(",");
            sb.append(StringEscapeUtils.escapeCsv(sf.format(comment.getSnippet().getPublishedAt().getValue())));
            sb.append(",");
            sb.append(StringEscapeUtils.escapeCsv(comment.getSnippet().getLikeCount().toString()));
            sb.append(",");
            sb.append(StringEscapeUtils.escapeCsv(comment.getSnippet().getTextDisplay()));
            sb.append("\n");
        });

        out.write(sb.toString());
        out.flush();
        out.close();
    }
}
