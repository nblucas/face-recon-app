package dev.nblucas.facialreconbackend.user.dto;

import org.springframework.http.MediaType;

public record UserPictureResponse(byte[] bytes, MediaType contentType) {
}
