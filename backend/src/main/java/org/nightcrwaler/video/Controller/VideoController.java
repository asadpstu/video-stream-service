package org.nightcrwaler.video.Controller;

import org.nightcrwaler.video.CustomMessage;
import org.nightcrwaler.video.entities.Video;
import org.nightcrwaler.video.service.VideoService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/videos")
@CrossOrigin({"http://localhost:5173", "http://localhost:3000", "http://localhost:8000"})
public class VideoController {

    @Value("${file.video.hsl}")
    private String hlsDirectory;

    private final VideoService videoService;

    public VideoController(VideoService videoService) {
        this.videoService = videoService;
    }

    @PostMapping
    public ResponseEntity<?> uploadVideo(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam("description") String description) {

        Video video = new Video();
        video.setTitle(title);
        video.setDescription(description);
        video.setVideoId(UUID.randomUUID().toString());

        Video savedVideo = videoService.save(video, file);

        if (savedVideo != null) {
            return ResponseEntity.ok(savedVideo);
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CustomMessage.builder().message("Video not uploaded").success(false).build());
        }
    }

    @GetMapping
    public List<Video> getAllVideos() {
        return videoService.getAll();
    }

    @GetMapping("/{videoId}/master.m3u8")
    public ResponseEntity<Resource> getMasterPlaylist(@PathVariable String videoId) {
        Path path = Paths.get(hlsDirectory, videoId, "master.m3u8");
        if (!Files.exists(path)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "application/vnd.apple.mpegurl")
                .body(new FileSystemResource(path));
    }

    @GetMapping("/{videoId}/{playlistName}.m3u8")
    public ResponseEntity<Resource> getVariantPlaylist(
            @PathVariable String videoId,
            @PathVariable String playlistName) {

        Path path = Paths.get(hlsDirectory, videoId, playlistName + ".m3u8");
        if (!Files.exists(path)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "application/vnd.apple.mpegurl")
                .body(new FileSystemResource(path));
    }

    @GetMapping("/{videoId}/{variant}/{segment}.ts")
    public ResponseEntity<Resource> getSegment(
            @PathVariable String videoId,
            @PathVariable String variant,
            @PathVariable String segment) {

        Path path = Paths.get(hlsDirectory, videoId, variant, segment + ".ts");
        if (!Files.exists(path)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "video/mp2t")
                .body(new FileSystemResource(path));
    }

    @GetMapping("/key/{videoId}")
    public ResponseEntity<byte[]> getEncryptionKey(@PathVariable String videoId) {
        Video video = videoService.get(videoId);
        if (video == null || video.getEncryptionKeyHex() == null) {
            return ResponseEntity.notFound().build();
        }
        byte[] keyBytes = hexToBytes(video.getEncryptionKeyHex());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(keyBytes);
    }

    private byte[] hexToBytes(String hex) {
        int length = hex.length();
        byte[] data = new byte[length / 2];
        for (int i = 0; i < length; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
}
