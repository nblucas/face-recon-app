package dev.nblucas.facialreconbackend.repositories;

import dev.nblucas.facialreconbackend.jooq.tables.records.TbUsersRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import dev.nblucas.facialreconbackend.TestcontainersConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class UserRepositoryImplIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldPersistUserAndGenerateIdAndTimestamps() {
        TbUsersRecord created = userRepository.create("John Doe", "52998224725", "/pictures/john.png");

        assertThat(created.getCoSeqUser()).isNotNull();
        assertThat(created.getName()).isEqualTo("John Doe");
        assertThat(created.getCpf()).isEqualTo("52998224725");
        assertThat(created.getPicturePath()).isEqualTo("/pictures/john.png");
        assertThat(created.getCreatedAt()).isNotNull();
        assertThat(created.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldReturnTrueWhenCpfExists() {
        userRepository.create("Jane Doe", "11144477735", "/pictures/jane.png");

        assertThat(userRepository.exists("11144477735")).isTrue();
    }

    @Test
    void shouldReturnFalseWhenCpfDoesNotExist() {
        userRepository.create("Bruno Lima", "22233344459", "/pictures/bruno.png");
        userRepository.create("Carla Souza", "33344455567", "/pictures/carla.png");

        assertThat(userRepository.exists("99988877766")).isFalse();
    }

    @Test
    void shouldReturnTrueWhenIdExists() {
        TbUsersRecord created = userRepository.create("Mark Doe", "12345678909", "/pictures/mark.png");

        assertThat(userRepository.exists(created.getCoSeqUser())).isTrue();
    }

    @Test
    void shouldReturnFalseWhenIdDoesNotExist() {
        userRepository.create("Diego Alves", "44455566678", "/pictures/diego.png");
        userRepository.create("Elisa Nunes", "55566677789", "/pictures/elisa.png");

        assertThat(userRepository.exists(Long.MAX_VALUE)).isFalse();
    }

    @Test
    void shouldUpdateNameAndPicturePathAndBumpUpdatedAt() {
        TbUsersRecord created = userRepository.create("Alice Doe", "98765432100", "/pictures/alice.png");

        TbUsersRecord updated = userRepository.update(
                created.getCoSeqUser(), "Alice Smith", "/pictures/alice-smith.png");

        assertThat(updated.getCoSeqUser()).isEqualTo(created.getCoSeqUser());
        assertThat(updated.getName()).isEqualTo("Alice Smith");
        assertThat(updated.getCpf()).isEqualTo("98765432100");
        assertThat(updated.getPicturePath()).isEqualTo("/pictures/alice-smith.png");
        assertThat(updated.getCreatedAt()).isEqualTo(created.getCreatedAt());
        assertThat(updated.getUpdatedAt()).isAfterOrEqualTo(created.getUpdatedAt());
    }

    @Test
    void shouldReturnNullWhenUpdatingNonExistentId() {
        userRepository.create("Fabio Rocha", "66677788890", "/pictures/fabio.png");
        userRepository.create("Gisele Alves", "77788899901", "/pictures/gisele.png");

        TbUsersRecord updated = userRepository.update(Long.MAX_VALUE, "Ghost", "/pictures/ghost.png");

        assertThat(updated).isNull();
    }
}
