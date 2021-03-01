package com.personal.project.emailsender.dto;

import javax.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AttachmentDTO {

    @NotBlank
    private String name;

    @NotBlank @URL
    private String url;
}
