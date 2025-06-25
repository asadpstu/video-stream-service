package org.nightcrwaler.video.service;

import jakarta.annotation.PostConstruct;
import org.nightcrwaler.video.entities.Video;
import org.nightcrwaler.video.repository.VideoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;

@Service
public class VideoServiceImpl implements VideoService {

    @Value("${files.video}")
    private String uploadDir;

    @Value("${file.video.hsl}")
    private String hlsDir;

    @Value("${server.port}")
    private String serverPort;

    private final VideoRepository videoRepository;

    public VideoServiceImpl(VideoRepository videoRepository) {
        this.videoRepository = videoRepository;
    }

    @PostConstruct
    public void init() {
        File uploadFolder = new File(uploadDir);
        try {
            Files.createDirectories(Paths.get(hlsDir));
        } catch (IOException e) {
            throw new RuntimeException("Failed to create HLS directory", e);
        }
        if (!uploadFolder.exists()) {
            uploadFolder.mkdir();
        }
    }

    @Override
    public Video save(Video video, MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            String filename = StringUtils.cleanPath(file.getOriginalFilename());
            Path destinationPath = Paths.get(uploadDir, filename);

            Files.copy(inputStream, destinationPath, StandardCopyOption.REPLACE_EXISTING);

            video.setContentType(file.getContentType());
            video.setFilePath(destinationPath.toString());

            SecretKey aesKey = generateAesKey();
            byte[] iv = generateIv();

            video.setEncryptionKeyHex(bytesToHex(aesKey.getEncoded()));
            video.setEncryptionIvHex(bytesToHex(iv));

            Video savedVideo = videoRepository.save(video);

            processVideo(savedVideo.getVideoId());

            return savedVideo;

        } catch (IOException | NoSuchAlgorithmException | InterruptedException e) {
            throw new RuntimeException("Failed to save video", e);
        }
    }

    @Override
    public Video get(String videoId) {
        return videoRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video not found: " + videoId));
    }

    @Override
    public Video getByTitle(String title) {
        return null;
    }

    @Override
    public List<Video> getAll() {
        return videoRepository.findAll();
    }

    @Override
    public String processVideo(String videoId) throws IOException, InterruptedException {
        Video video = get(videoId);
        Path inputPath = Paths.get(video.getFilePath());
        Path outputDir = Paths.get(hlsDir, videoId);

        Files.createDirectories(outputDir);

        byte[] keyBytes = hexToBytes(video.getEncryptionKeyHex());
        Path keyFile = outputDir.resolve("enc.key");
        Files.write(keyFile, keyBytes);

        String keyUri = String.format("http://localhost:%s/api/v1/videos/key/%s", serverPort, videoId);
        String ivHex = video.getEncryptionIvHex();

        String keyInfoContent = String.format("%s\n%s\n%s", keyUri, keyFile.toString(), ivHex);
        Path keyInfoFile = outputDir.resolve("enc.keyinfo");
        Files.write(keyInfoFile, keyInfoContent.getBytes());

        List<String[]> variants = Arrays.asList(
                new String[]{"640x360", "800k", "128k", "360p"},
                new String[]{"1280x720", "2800k", "128k", "720p"},
                new String[]{"1920x1080", "5000k", "128k", "1080p"}
        );

        StringBuilder masterPlaylist = new StringBuilder("#EXTM3U\n#EXT-X-VERSION:3\n");

        for (String[] variant : variants) {
            String resolution = variant[0];
            String videoBitrate = variant[1];
            String audioBitrate = variant[2];
            String variantName = variant[3];

            Path variantDir = outputDir.resolve(variantName);
            Files.createDirectories(variantDir);

            String segmentBaseUrl = String.format("http://localhost:8000/api/v1/videos/%s/%s/", videoId, variantName);

            String ffmpegCmd = String.format(
                    "ffmpeg -i \"%s\" -preset veryfast -map 0:v:0 -map 0:a:0? " +
                            "-c:v libx264 -b:v %s -s:v %s " +
                            "-c:a aac -b:a %s " +
                            "-hls_time 10 -hls_playlist_type vod -hls_list_size 0 " +
                            "-hls_key_info_file \"%s\" " +
                            "-hls_segment_filename \"%s/%%03d.ts\" " +
                            "-hls_base_url \"%s\" " +
                            "\"%s/%s.m3u8\"",
                    inputPath, videoBitrate, resolution, audioBitrate,
                    keyInfoFile, variantDir, segmentBaseUrl, outputDir, variantName
            );

            ProcessBuilder pb = new ProcessBuilder("/bin/bash", "-c", ffmpegCmd);
            pb.inheritIO();
            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                throw new RuntimeException("FFmpeg processing failed for variant: " + variantName);
            }

            long totalBandwidth = Long.parseLong(videoBitrate.replace("k", "")) * 1000 +
                    Long.parseLong(audioBitrate.replace("k", "")) * 1000;

            masterPlaylist.append(String.format(
                    "#EXT-X-STREAM-INF:BANDWIDTH=%d,RESOLUTION=%s,CODECS=\"avc1.4d401f,mp4a.40.2\"\n%s.m3u8\n",
                    totalBandwidth, resolution, variantName));
        }

        Path masterPlaylistPath = outputDir.resolve("master.m3u8");
        Files.write(masterPlaylistPath, masterPlaylist.toString().getBytes());

        Files.deleteIfExists(keyFile);
        Files.deleteIfExists(keyInfoFile);

        return videoId;
    }

    private SecretKey generateAesKey() throws NoSuchAlgorithmException {
        KeyGenerator generator = KeyGenerator.getInstance("AES");
        generator.init(128);
        return generator.generateKey();
    }

    private byte[] generateIv() {
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);
        return iv;
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
}
