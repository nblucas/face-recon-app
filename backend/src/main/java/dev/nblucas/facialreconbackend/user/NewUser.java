package dev.nblucas.facialreconbackend.user;

record NewUser(String name, String cpf, String picturePath, Float[] embedding) {
}
