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
}
