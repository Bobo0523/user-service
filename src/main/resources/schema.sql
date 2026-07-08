CREATE TABLE public.users (
                              id int4 GENERATED ALWAYS AS IDENTITY( INCREMENT BY 1 MINVALUE 1 MAXVALUE 2147483647 START 1 CACHE 1 NO CYCLE) NOT NULL,
                              username varchar NULL,
                              "password" varchar NULL,
                              email varchar NULL,
                              CONSTRAINT users_pk PRIMARY KEY (id),
                              CONSTRAINT users_username_key UNIQUE (username)
);