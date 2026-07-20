package dev.nblucas.facialreconbackend.repositories;

import dev.nblucas.facialreconbackend.jooq.tables.records.TbUsersRecord;

import java.util.List;

public interface UserRepository {
    public TbUsersRecord create(String name, String cpf, String picturePath);
    public boolean exists(String cpf);
    public boolean exists(Long id);
    public TbUsersRecord update(Long id, String name, String picturePath);
    public List<TbUsersRecord> findAll(int offset, int limit);
    public long count();
}
