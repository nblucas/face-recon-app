package dev.nblucas.facialreconbackend.dtos;

import org.springframework.http.MediaType;

public record UserPictureResponse(byte[] bytes, MediaType contentType) {
}
