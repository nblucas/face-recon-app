package dev.nblucas.facialreconbackend.controllers;

import dev.nblucas.facialreconbackend.dtos.CreateUserRequest;
import dev.nblucas.facialreconbackend.dtos.UpdateUserRequest;
import dev.nblucas.facialreconbackend.dtos.UserPageResponse;
import dev.nblucas.facialreconbackend.dtos.UserPictureResponse;
import dev.nblucas.facialreconbackend.dtos.UserResponse;
import dev.nblucas.facialreconbackend.services.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController()
@RequestMapping("api/v1/users")
public class UserController {

    UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping(
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<UserResponse> createUser(
            @Valid @RequestPart CreateUserRequest request,
            @RequestPart("picture") MultipartFile picture
    ) {
        UserResponse user = this.userService.create(request, picture);
        return new ResponseEntity<>(user, HttpStatus.CREATED);
    }

    @PutMapping(
            path = "/{id}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @Valid @RequestPart UpdateUserRequest request,
            @RequestPart(value = "picture", required = false) MultipartFile picture
    ) {
        UserResponse user = this.userService.update(id, request, picture);
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserPageResponse> listUsers(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "20") int limit
    ) {
        UserPageResponse page = this.userService.list(offset, limit);
        return new ResponseEntity<>(page, HttpStatus.OK);
    }

    @GetMapping(path = "/{id}/picture")
    public ResponseEntity<byte[]> getUserPicture(@PathVariable Long id) {
        UserPictureResponse picture = this.userService.getPicture(id);
        return ResponseEntity.ok().contentType(picture.contentType()).body(picture.bytes());
    }

    @DeleteMapping(path = "/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        this.userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
