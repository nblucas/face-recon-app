package dev.nblucas.facialreconbackend.user;

import dev.nblucas.facialreconbackend.user.dto.CreateUserRequest;
import dev.nblucas.facialreconbackend.user.dto.IdentifyUserResponse;
import dev.nblucas.facialreconbackend.user.dto.UpdateUserRequest;
import dev.nblucas.facialreconbackend.user.dto.UserPageResponse;
import dev.nblucas.facialreconbackend.user.dto.UserPictureResponse;
import dev.nblucas.facialreconbackend.user.dto.UserResponse;
import dev.nblucas.facialreconbackend.user.dto.VerifyUserRequest;
import dev.nblucas.facialreconbackend.user.dto.VerifyUserResponse;
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

    @GetMapping(path = "/{id}")
    public ResponseEntity<UserResponse> getUser(@PathVariable Long id) {
        UserResponse user = this.userService.get(id);
        return new ResponseEntity<>(user, HttpStatus.OK);
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

    @PostMapping(
            path = "/identify",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<IdentifyUserResponse> identifyUser(@RequestPart("picture") MultipartFile picture) {
        IdentifyUserResponse response = this.userService.identify(picture);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping(
            path = "/verify",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<VerifyUserResponse> verifyUser(
            @Valid @RequestPart VerifyUserRequest request,
            @RequestPart("picture") MultipartFile picture
    ) {
        VerifyUserResponse response = this.userService.verify(request.cpf(), picture);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
