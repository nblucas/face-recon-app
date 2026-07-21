package dev.nblucas.facialreconbackend.user;

import dev.nblucas.facialreconbackend.jooq.tables.records.TbUsersRecord;

import java.util.List;
import java.util.Optional;

public interface UserRepository {
    public TbUsersRecord create(String name, String cpf, String picturePath, Float[] embedding);
    public boolean exists(String cpf);
    public boolean exists(Long id);
    public Optional<TbUsersRecord> update(Long id, String name, String picturePath, Float[] embedding);
    public List<TbUsersRecord> findAll(int offset, int limit);
    public long count();
    public Optional<TbUsersRecord> findById(Long id);
    public Optional<TbUsersRecord> findByCpf(String cpf);
    public void delete(Long id);
}
