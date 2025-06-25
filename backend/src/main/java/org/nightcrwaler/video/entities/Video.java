package org.nightcrwaler.video.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Video {

    @Id
    private String videoId;
    private String title;
    private String description;
    private String filePath;
    private String contentType;

    private String encryptionKeyHex; // Hex string of the encryption key
    private String encryptionIvHex;  // Hex string of the encryption IV (Initialization Vector)
}
