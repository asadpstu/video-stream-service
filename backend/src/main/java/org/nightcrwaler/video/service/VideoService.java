package org.nightcrwaler.video.service;
import org.nightcrwaler.video.entities.Video;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface VideoService {

    Video save(Video video, MultipartFile file);

    Video get(String videoId);


    Video getByTitle(String title);

    List<Video> getAll();

    String processVideo(String videoId) throws IOException, InterruptedException;


}
