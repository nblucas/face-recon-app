package dev.nblucas.facialreconbackend.repositories;

import dev.nblucas.facialreconbackend.jooq.tables.records.TbUsersRecord;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;

import static dev.nblucas.facialreconbackend.jooq.Tables.TB_USERS;

@Repository
public class UserRepositoryImpl implements UserRepository {
    private final DSLContext dsl;

    public UserRepositoryImpl(DSLContext dsl) {
        this.dsl = dsl;
    }

    public TbUsersRecord create(String name, String cpf, String picturePath) {
        OffsetDateTime now = OffsetDateTime.now();

        return this.dsl
                .insertInto(TB_USERS)
                .set(TB_USERS.NAME, name)
                .set(TB_USERS.CPF, cpf)
                .set(TB_USERS.PICTURE_PATH, picturePath)
                .set(TB_USERS.CREATED_AT, now)
                .set(TB_USERS.UPDATED_AT, now)
                .returning()
                .fetchOne();
    }

    public boolean exists(String cpf) {
        Condition condition = TB_USERS.CPF.eq(cpf);
        return this.dsl.fetchCount(TB_USERS, condition) > 0;
    }
}
