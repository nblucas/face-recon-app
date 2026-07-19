package dev.nblucas.facialreconbackend.repositories;

import dev.nblucas.facialreconbackend.jooq.tables.records.TbUsersRecord;

public interface UserRepository {
    public TbUsersRecord create(String name, String cpf, String picturePath);
    public boolean exists(String cpf);
    public boolean exists(Long id);
    public TbUsersRecord update(Long id, String name, String picturePath);
}
