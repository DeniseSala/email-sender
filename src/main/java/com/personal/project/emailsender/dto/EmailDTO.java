package com.personal.project.emailsender.dto;

import javax.validation.Valid;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.lang.Nullable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailDTO {

    @NotBlank @Email
    private String from;

    @NotBlank @Email
    private String to;

    @NotBlank
    private String subject;

    @NotBlank
    private String body;

    @Nullable @Valid
    private AttachmentDTO attachment;
}
