package dev.nblucas.facialreconbackend.repositories;

import dev.nblucas.facialreconbackend.jooq.tables.records.TbUsersRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import dev.nblucas.facialreconbackend.TestcontainersConfiguration;

import java.util.List;
import java.util.Optional;

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

        Optional<TbUsersRecord> updated = userRepository.update(
                created.getCoSeqUser(), "Alice Smith", "/pictures/alice-smith.png");

        assertThat(updated).isPresent();
        assertThat(updated.get().getCoSeqUser()).isEqualTo(created.getCoSeqUser());
        assertThat(updated.get().getName()).isEqualTo("Alice Smith");
        assertThat(updated.get().getCpf()).isEqualTo("98765432100");
        assertThat(updated.get().getPicturePath()).isEqualTo("/pictures/alice-smith.png");
        assertThat(updated.get().getCreatedAt()).isEqualTo(created.getCreatedAt());
        assertThat(updated.get().getUpdatedAt()).isAfterOrEqualTo(created.getUpdatedAt());
    }

    @Test
    void shouldReturnEmptyWhenUpdatingNonExistentId() {
        userRepository.create("Fabio Rocha", "66677788890", "/pictures/fabio.png");
        userRepository.create("Gisele Alves", "77788899901", "/pictures/gisele.png");

        Optional<TbUsersRecord> updated = userRepository.update(Long.MAX_VALUE, "Ghost", "/pictures/ghost.png");

        assertThat(updated).isEmpty();
    }

    @Test
    void shouldReturnUsersOrderedByCreationOrder() {
        TbUsersRecord first = userRepository.create("Helio Prado", "12312312312", "/pictures/helio.png");
        TbUsersRecord second = userRepository.create("Isabela Melo", "32132132132", "/pictures/isabela.png");
        TbUsersRecord third = userRepository.create("Joaquim Reis", "45645645645", "/pictures/joaquim.png");
        List<Long> ids = List.of(first.getCoSeqUser(), second.getCoSeqUser(), third.getCoSeqUser());

        List<TbUsersRecord> page = userRepository.findAll(0, Integer.MAX_VALUE).stream()
                .filter(user -> ids.contains(user.getCoSeqUser()))
                .toList();

        assertThat(page).containsExactly(first, second, third);
    }

    @Test
    void shouldRespectOffsetAndLimitWhenPaginating() {
        TbUsersRecord first = userRepository.create("Karen Dias", "65465465465", "/pictures/karen.png");
        TbUsersRecord second = userRepository.create("Lucas Prado", "78978978978", "/pictures/lucas.png");
        userRepository.create("Mariana Costa", "89189189189", "/pictures/mariana.png");

        int firstIndex = userRepository.findAll(0, Integer.MAX_VALUE).indexOf(first);
        List<TbUsersRecord> page = userRepository.findAll(firstIndex + 1, 1);

        assertThat(page).containsExactly(second);
    }

    @Test
    void shouldReturnEmptyListWhenOffsetExceedsTotalUsers() {
        long total = userRepository.count();

        List<TbUsersRecord> page = userRepository.findAll((int) total + 1000, 20);

        assertThat(page).isEmpty();
    }

    @Test
    void shouldReturnTotalCountOfUsers() {
        long before = userRepository.count();

        userRepository.create("Nadia Fontes", "91291291291", "/pictures/nadia.png");
        userRepository.create("Otavio Reis", "92392392392", "/pictures/otavio.png");

        assertThat(userRepository.count()).isEqualTo(before + 2);
    }
}
