CREATE SEQUENCE seq_tb_users
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE tb_users (
    co_seq_user BIGINT PRIMARY KEY DEFAULT nextval('seq_tb_users'),
    name VARCHAR(255) NOT NULL,
    cpf VARCHAR(11) NOT NULL UNIQUE,
    picture_path VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

ALTER SEQUENCE seq_tb_users OWNED BY tb_users.co_seq_user;