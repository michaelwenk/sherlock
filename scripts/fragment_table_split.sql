CREATE TABLE fragment_record_1 (id integer PRIMARY KEY NOT NULL, n_bits bigint NOT NULL, nucleus character varying(255) NOT NULL, set_bits bit(209), signal_count integer NOT NULL, sub_data_set_string text NOT NULL);
INSERT INTO fragment_record_1 (id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string)
SELECT id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string
FROM fragment_record
WHERE id <= 500000;

CREATE TABLE fragment_record_2 (id integer PRIMARY KEY NOT NULL, n_bits bigint NOT NULL, nucleus character varying(255) NOT NULL, set_bits bit(209), signal_count integer NOT NULL, sub_data_set_string text NOT NULL);
INSERT INTO fragment_record_2 (id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string)
SELECT id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string
FROM fragment_record
WHERE id > 500000 AND id <= 1000000;

CREATE TABLE fragment_record_3 (id integer PRIMARY KEY NOT NULL, n_bits bigint NOT NULL, nucleus character varying(255) NOT NULL, set_bits bit(209), signal_count integer NOT NULL, sub_data_set_string text NOT NULL);
INSERT INTO fragment_record_3 (id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string)
SELECT id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string
FROM fragment_record
WHERE id > 1000000 AND id <= 1500000;

CREATE TABLE fragment_record_4 (id integer PRIMARY KEY NOT NULL, n_bits bigint NOT NULL, nucleus character varying(255) NOT NULL, set_bits bit(209), signal_count integer NOT NULL, sub_data_set_string text NOT NULL);
INSERT INTO fragment_record_4 (id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string)
SELECT id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string
FROM fragment_record
WHERE id > 1500000 AND id <= 2000000;

CREATE TABLE fragment_record_5 (id integer PRIMARY KEY NOT NULL, n_bits bigint NOT NULL, nucleus character varying(255) NOT NULL, set_bits bit(209), signal_count integer NOT NULL, sub_data_set_string text NOT NULL);
INSERT INTO fragment_record_5 (id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string)
SELECT id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string
FROM fragment_record
WHERE id > 2000000 AND id <= 2500000;

CREATE TABLE fragment_record_6 (id integer PRIMARY KEY NOT NULL, n_bits bigint NOT NULL, nucleus character varying(255) NOT NULL, set_bits bit(209), signal_count integer NOT NULL, sub_data_set_string text NOT NULL);
INSERT INTO fragment_record_6 (id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string)
SELECT id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string
FROM fragment_record
WHERE id > 2500000 AND id <= 3000000;

CREATE TABLE fragment_record_7 (id integer PRIMARY KEY NOT NULL, n_bits bigint NOT NULL, nucleus character varying(255) NOT NULL, set_bits bit(209), signal_count integer NOT NULL, sub_data_set_string text NOT NULL);
INSERT INTO fragment_record_7 (id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string)
SELECT id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string
FROM fragment_record
WHERE id > 3000000 AND id <= 3500000;

CREATE TABLE fragment_record_8 (id integer PRIMARY KEY NOT NULL, n_bits bigint NOT NULL, nucleus character varying(255) NOT NULL, set_bits bit(209), signal_count integer NOT NULL, sub_data_set_string text NOT NULL);
INSERT INTO fragment_record_8 (id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string)
SELECT id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string
FROM fragment_record
WHERE id > 3500000 AND id <= 4000000;

CREATE TABLE fragment_record_9 (id integer PRIMARY KEY NOT NULL, n_bits bigint NOT NULL, nucleus character varying(255) NOT NULL, set_bits bit(209), signal_count integer NOT NULL, sub_data_set_string text NOT NULL);
INSERT INTO fragment_record_9 (id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string)
SELECT id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string
FROM fragment_record
WHERE id > 4000000 AND id <= 4500000;

CREATE TABLE fragment_record_10 (id integer PRIMARY KEY NOT NULL, n_bits bigint NOT NULL, nucleus character varying(255) NOT NULL, set_bits bit(209), signal_count integer NOT NULL, sub_data_set_string text NOT NULL);
INSERT INTO fragment_record_10 (id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string)
SELECT id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string
FROM fragment_record
WHERE id > 4500000 AND id <= 5000000;

CREATE TABLE fragment_record_11 (id integer PRIMARY KEY NOT NULL, n_bits bigint NOT NULL, nucleus character varying(255) NOT NULL, set_bits bit(209), signal_count integer NOT NULL, sub_data_set_string text NOT NULL);
INSERT INTO fragment_record_11 (id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string)
SELECT id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string
FROM fragment_record
WHERE id > 5000000 AND id <= 5500000;

CREATE TABLE fragment_record_12 (id integer PRIMARY KEY NOT NULL, n_bits bigint NOT NULL, nucleus character varying(255) NOT NULL, set_bits bit(209), signal_count integer NOT NULL, sub_data_set_string text NOT NULL);
INSERT INTO fragment_record_12 (id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string)
SELECT id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string
FROM fragment_record
WHERE id > 5500000 AND id <= 6000000;

CREATE TABLE fragment_record_13 (id integer PRIMARY KEY NOT NULL, n_bits bigint NOT NULL, nucleus character varying(255) NOT NULL, set_bits bit(209), signal_count integer NOT NULL, sub_data_set_string text NOT NULL);
INSERT INTO fragment_record_13 (id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string)
SELECT id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string
FROM fragment_record
WHERE id > 6000000 AND id <= 6500000;

CREATE TABLE fragment_record_14 (id integer PRIMARY KEY NOT NULL, n_bits bigint NOT NULL, nucleus character varying(255) NOT NULL, set_bits bit(209), signal_count integer NOT NULL, sub_data_set_string text NOT NULL);
INSERT INTO fragment_record_14 (id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string)
SELECT id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string
FROM fragment_record
WHERE id > 6500000 AND id <= 7000000;

CREATE TABLE fragment_record_15 (id integer PRIMARY KEY NOT NULL, n_bits bigint NOT NULL, nucleus character varying(255) NOT NULL, set_bits bit(209), signal_count integer NOT NULL, sub_data_set_string text NOT NULL);
INSERT INTO fragment_record_15 (id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string)
SELECT id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string
FROM fragment_record
WHERE id > 7000000 AND id <= 7500000;

CREATE TABLE fragment_record_16 (id integer PRIMARY KEY NOT NULL, n_bits bigint NOT NULL, nucleus character varying(255) NOT NULL, set_bits bit(209), signal_count integer NOT NULL, sub_data_set_string text NOT NULL);
INSERT INTO fragment_record_16 (id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string)
SELECT id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string
FROM fragment_record
WHERE id > 7500000 AND id <= 8000000;

CREATE TABLE fragment_record_17 (id integer PRIMARY KEY NOT NULL, n_bits bigint NOT NULL, nucleus character varying(255) NOT NULL, set_bits bit(209), signal_count integer NOT NULL, sub_data_set_string text NOT NULL);
INSERT INTO fragment_record_17 (id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string)
SELECT id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string
FROM fragment_record
WHERE id > 8000000 AND id <= 8500000;

CREATE TABLE fragment_record_18 (id integer PRIMARY KEY NOT NULL, n_bits bigint NOT NULL, nucleus character varying(255) NOT NULL, set_bits bit(209), signal_count integer NOT NULL, sub_data_set_string text NOT NULL);
INSERT INTO fragment_record_18 (id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string)
SELECT id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string
FROM fragment_record
WHERE id > 8500000 AND id <= 9000000;

CREATE TABLE fragment_record_19 (id integer PRIMARY KEY NOT NULL, n_bits bigint NOT NULL, nucleus character varying(255) NOT NULL, set_bits bit(209), signal_count integer NOT NULL, sub_data_set_string text NOT NULL);
INSERT INTO fragment_record_19 (id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string)
SELECT id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string
FROM fragment_record
WHERE id > 9000000 AND id <= 9500000;

CREATE TABLE fragment_record_20 (id integer PRIMARY KEY NOT NULL, n_bits bigint NOT NULL, nucleus character varying(255) NOT NULL, set_bits bit(209), signal_count integer NOT NULL, sub_data_set_string text NOT NULL);
INSERT INTO fragment_record_20 (id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string)
SELECT id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string
FROM fragment_record
WHERE id > 9500000 AND id <= 10000000;

CREATE TABLE fragment_record_21 (id integer PRIMARY KEY NOT NULL, n_bits bigint NOT NULL, nucleus character varying(255) NOT NULL, set_bits bit(209), signal_count integer NOT NULL, sub_data_set_string text NOT NULL);
INSERT INTO fragment_record_21 (id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string)
SELECT id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string
FROM fragment_record
WHERE id > 10000000 AND id <= 10500000;

CREATE TABLE fragment_record_22 (id integer PRIMARY KEY NOT NULL, n_bits bigint NOT NULL, nucleus character varying(255) NOT NULL, set_bits bit(209), signal_count integer NOT NULL, sub_data_set_string text NOT NULL);
INSERT INTO fragment_record_22 (id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string)
SELECT id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string
FROM fragment_record
WHERE id > 10500000 AND id <= 11000000;

CREATE TABLE fragment_record_23 (id integer PRIMARY KEY NOT NULL, n_bits bigint NOT NULL, nucleus character varying(255) NOT NULL, set_bits bit(209), signal_count integer NOT NULL, sub_data_set_string text NOT NULL);
INSERT INTO fragment_record_23 (id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string)
SELECT id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string
FROM fragment_record
WHERE id > 11000000 AND id <= 11500000;

CREATE TABLE fragment_record_24 (id integer PRIMARY KEY NOT NULL, n_bits bigint NOT NULL, nucleus character varying(255) NOT NULL, set_bits bit(209), signal_count integer NOT NULL, sub_data_set_string text NOT NULL);
INSERT INTO fragment_record_24 (id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string)
SELECT id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string
FROM fragment_record
WHERE id > 11500000 AND id <= 12000000;

CREATE TABLE fragment_record_25 (id integer PRIMARY KEY NOT NULL, n_bits bigint NOT NULL, nucleus character varying(255) NOT NULL, set_bits bit(209), signal_count integer NOT NULL, sub_data_set_string text NOT NULL);
INSERT INTO fragment_record_25 (id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string)
SELECT id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string
FROM fragment_record
WHERE id > 12000000 AND id <= 12500000;

CREATE TABLE fragment_record_26 (id integer PRIMARY KEY NOT NULL, n_bits bigint NOT NULL, nucleus character varying(255) NOT NULL, set_bits bit(209), signal_count integer NOT NULL, sub_data_set_string text NOT NULL);
INSERT INTO fragment_record_26 (id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string)
SELECT id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string
FROM fragment_record
WHERE id > 12500000 AND id <= 13000000;

CREATE TABLE fragment_record_27 (id integer PRIMARY KEY NOT NULL, n_bits bigint NOT NULL, nucleus character varying(255) NOT NULL, set_bits bit(209), signal_count integer NOT NULL, sub_data_set_string text NOT NULL);
INSERT INTO fragment_record_27 (id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string)
SELECT id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string
FROM fragment_record
WHERE id > 13000000 AND id <= 13500000;

CREATE TABLE fragment_record_28 (id integer PRIMARY KEY NOT NULL, n_bits bigint NOT NULL, nucleus character varying(255) NOT NULL, set_bits bit(209), signal_count integer NOT NULL, sub_data_set_string text NOT NULL);
INSERT INTO fragment_record_28 (id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string)
SELECT id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string
FROM fragment_record
WHERE id > 13500000 AND id <= 14000000;

CREATE TABLE fragment_record_29 (id integer PRIMARY KEY NOT NULL, n_bits bigint NOT NULL, nucleus character varying(255) NOT NULL, set_bits bit(209), signal_count integer NOT NULL, sub_data_set_string text NOT NULL);
INSERT INTO fragment_record_29 (id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string)
SELECT id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string
FROM fragment_record
WHERE id > 14000000 AND id <= 14500000;

CREATE TABLE fragment_record_30 (id integer PRIMARY KEY NOT NULL, n_bits bigint NOT NULL, nucleus character varying(255) NOT NULL, set_bits bit(209), signal_count integer NOT NULL, sub_data_set_string text NOT NULL);
INSERT INTO fragment_record_30 (id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string)
SELECT id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string
FROM fragment_record
WHERE id > 14500000 AND id <= 15000000;

CREATE TABLE fragment_record_31 (id integer PRIMARY KEY NOT NULL, n_bits bigint NOT NULL, nucleus character varying(255) NOT NULL, set_bits bit(209), signal_count integer NOT NULL, sub_data_set_string text NOT NULL);
INSERT INTO fragment_record_31 (id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string)
SELECT id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string
FROM fragment_record
WHERE id > 15000000 AND id <= 15500000;

CREATE TABLE fragment_record_32 (id integer PRIMARY KEY NOT NULL, n_bits bigint NOT NULL, nucleus character varying(255) NOT NULL, set_bits bit(209), signal_count integer NOT NULL, sub_data_set_string text NOT NULL);
INSERT INTO fragment_record_32 (id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string)
SELECT id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string
FROM fragment_record
WHERE id > 15500000 AND id <= 16000000;

CREATE TABLE fragment_record_33 (id integer PRIMARY KEY NOT NULL, n_bits bigint NOT NULL, nucleus character varying(255) NOT NULL, set_bits bit(209), signal_count integer NOT NULL, sub_data_set_string text NOT NULL);
INSERT INTO fragment_record_33 (id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string)
SELECT id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string
FROM fragment_record
WHERE id > 16000000 AND id <= 16500000;

CREATE TABLE fragment_record_34 (id integer PRIMARY KEY NOT NULL, n_bits bigint NOT NULL, nucleus character varying(255) NOT NULL, set_bits bit(209), signal_count integer NOT NULL, sub_data_set_string text NOT NULL);
INSERT INTO fragment_record_34 (id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string)
SELECT id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string
FROM fragment_record
WHERE id > 16500000 AND id <= 17000000;

CREATE TABLE fragment_record_35 (id integer PRIMARY KEY NOT NULL, n_bits bigint NOT NULL, nucleus character varying(255) NOT NULL, set_bits bit(209), signal_count integer NOT NULL, sub_data_set_string text NOT NULL);
INSERT INTO fragment_record_35 (id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string)
SELECT id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string
FROM fragment_record
WHERE id > 17000000 AND id <= 17500000;

CREATE TABLE fragment_record_36 (id integer PRIMARY KEY NOT NULL, n_bits bigint NOT NULL, nucleus character varying(255) NOT NULL, set_bits bit(209), signal_count integer NOT NULL, sub_data_set_string text NOT NULL);
INSERT INTO fragment_record_36 (id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string)
SELECT id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string
FROM fragment_record
WHERE id > 17500000 AND id <= 18000000;

CREATE TABLE fragment_record_37 (id integer PRIMARY KEY NOT NULL, n_bits bigint NOT NULL, nucleus character varying(255) NOT NULL, set_bits bit(209), signal_count integer NOT NULL, sub_data_set_string text NOT NULL);
INSERT INTO fragment_record_37 (id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string)
SELECT id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string
FROM fragment_record
WHERE id > 18000000 AND id <= 18500000;

CREATE TABLE fragment_record_38 (id integer PRIMARY KEY NOT NULL, n_bits bigint NOT NULL, nucleus character varying(255) NOT NULL, set_bits bit(209), signal_count integer NOT NULL, sub_data_set_string text NOT NULL);
INSERT INTO fragment_record_38 (id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string)
SELECT id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string
FROM fragment_record
WHERE id > 18500000 AND id <= 19000000;

CREATE TABLE fragment_record_39 (id integer PRIMARY KEY NOT NULL, n_bits bigint NOT NULL, nucleus character varying(255) NOT NULL, set_bits bit(209), signal_count integer NOT NULL, sub_data_set_string text NOT NULL);
INSERT INTO fragment_record_39 (id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string)
SELECT id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string
FROM fragment_record
WHERE id > 19000000 AND id <= 19500000;

CREATE TABLE fragment_record_40 (id integer PRIMARY KEY NOT NULL, n_bits bigint NOT NULL, nucleus character varying(255) NOT NULL, set_bits bit(209), signal_count integer NOT NULL, sub_data_set_string text NOT NULL);
INSERT INTO fragment_record_40 (id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string)
SELECT id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string
FROM fragment_record
WHERE id > 19500000 AND id <= 20000000;

CREATE TABLE fragment_record_41 (id integer PRIMARY KEY NOT NULL, n_bits bigint NOT NULL, nucleus character varying(255) NOT NULL, set_bits bit(209), signal_count integer NOT NULL, sub_data_set_string text NOT NULL);
INSERT INTO fragment_record_41 (id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string)
SELECT id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string
FROM fragment_record
WHERE id > 20000000 AND id <= 20500000;

CREATE TABLE fragment_record_42 (id integer PRIMARY KEY NOT NULL, n_bits bigint NOT NULL, nucleus character varying(255) NOT NULL, set_bits bit(209), signal_count integer NOT NULL, sub_data_set_string text NOT NULL);
INSERT INTO fragment_record_42 (id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string)
SELECT id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string
FROM fragment_record
WHERE id > 20500000 AND id <= 21000000;

CREATE TABLE fragment_record_43 (id integer PRIMARY KEY NOT NULL, n_bits bigint NOT NULL, nucleus character varying(255) NOT NULL, set_bits bit(209), signal_count integer NOT NULL, sub_data_set_string text NOT NULL);
INSERT INTO fragment_record_43 (id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string)
SELECT id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string
FROM fragment_record
WHERE id > 21000000 AND id <= 21500000;

CREATE TABLE fragment_record_44 (id integer PRIMARY KEY NOT NULL, n_bits bigint NOT NULL, nucleus character varying(255) NOT NULL, set_bits bit(209), signal_count integer NOT NULL, sub_data_set_string text NOT NULL);
INSERT INTO fragment_record_44 (id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string)
SELECT id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string
FROM fragment_record
WHERE id > 21500000 AND id <= 22000000;

CREATE TABLE fragment_record_45 (id integer PRIMARY KEY NOT NULL, n_bits bigint NOT NULL, nucleus character varying(255) NOT NULL, set_bits bit(209), signal_count integer NOT NULL, sub_data_set_string text NOT NULL);
INSERT INTO fragment_record_45 (id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string)
SELECT id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string
FROM fragment_record
WHERE id > 22000000 AND id <= 22500000;

CREATE TABLE fragment_record_46 (id integer PRIMARY KEY NOT NULL, n_bits bigint NOT NULL, nucleus character varying(255) NOT NULL, set_bits bit(209), signal_count integer NOT NULL, sub_data_set_string text NOT NULL);
INSERT INTO fragment_record_46 (id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string)
SELECT id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string
FROM fragment_record
WHERE id > 22500000 AND id <= 23000000;

CREATE TABLE fragment_record_47 (id integer PRIMARY KEY NOT NULL, n_bits bigint NOT NULL, nucleus character varying(255) NOT NULL, set_bits bit(209), signal_count integer NOT NULL, sub_data_set_string text NOT NULL);
INSERT INTO fragment_record_47 (id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string)
SELECT id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string
FROM fragment_record
WHERE id > 23000000 AND id <= 23500000;

CREATE TABLE fragment_record_48 (id integer PRIMARY KEY NOT NULL, n_bits bigint NOT NULL, nucleus character varying(255) NOT NULL, set_bits bit(209), signal_count integer NOT NULL, sub_data_set_string text NOT NULL);
INSERT INTO fragment_record_48 (id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string)
SELECT id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string
FROM fragment_record
WHERE id > 23500000 AND id <= 24000000;

CREATE TABLE fragment_record_49 (id integer PRIMARY KEY NOT NULL, n_bits bigint NOT NULL, nucleus character varying(255) NOT NULL, set_bits bit(209), signal_count integer NOT NULL, sub_data_set_string text NOT NULL);
INSERT INTO fragment_record_49 (id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string)
SELECT id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string
FROM fragment_record
WHERE id > 24000000 AND id <= 24500000;

CREATE TABLE fragment_record_50 (id integer PRIMARY KEY NOT NULL, n_bits bigint NOT NULL, nucleus character varying(255) NOT NULL, set_bits bit(209), signal_count integer NOT NULL, sub_data_set_string text NOT NULL);
INSERT INTO fragment_record_50 (id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string)
SELECT id, n_bits, nucleus, set_bits, signal_count, sub_data_set_string
FROM fragment_record
WHERE id > 24500000 AND id <= 25000000;

