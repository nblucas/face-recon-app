package dev.nblucas.facialreconbackend.repositories;

import dev.nblucas.facialreconbackend.jooq.tables.records.TbUsersRecord;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

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

    public boolean exists(Long id) {
        Condition condition = TB_USERS.CO_SEQ_USER.eq(id);
        return this.dsl.fetchCount(TB_USERS, condition) > 0;
    }

    public TbUsersRecord update(Long id, String name, String picturePath) {
        return this.dsl
                .update(TB_USERS)
                .set(TB_USERS.NAME, name)
                .set(TB_USERS.PICTURE_PATH, picturePath)
                .set(TB_USERS.UPDATED_AT, OffsetDateTime.now())
                .where(TB_USERS.CO_SEQ_USER.eq(id))
                .returning()
                .fetchOne();
    }

    public List<TbUsersRecord> findAll(int offset, int limit) {
        return this.dsl
                .selectFrom(TB_USERS)
                .orderBy(TB_USERS.CO_SEQ_USER)
                .offset(offset)
                .limit(limit)
                .fetch();
    }

    public long count() {
        return this.dsl.fetchCount(TB_USERS);
    }

    public Optional<TbUsersRecord> findById(Long id) {
        return this.dsl
                .selectFrom(TB_USERS)
                .where(TB_USERS.CO_SEQ_USER.eq(id))
                .fetchOptional();
    }
}
