package org.nightcrwaler.video;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class CustomMessage {

    private String message;

    private boolean success = false;
}